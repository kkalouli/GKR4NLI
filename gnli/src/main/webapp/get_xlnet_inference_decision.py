# -*- coding: utf-8 -*-
import tensorflow as tf

device_name = tf.test.gpu_device_name()
if device_name != '/device:GPU:0':
  raise SystemError('GPU device not found')
#print('Found GPU at: {}'.format(device_name))


# Commented out IPython magic to ensure Python compatibility.
import torch
from torch.utils.data import TensorDataset, DataLoader, RandomSampler, SequentialSampler
from keras.preprocessing.sequence import pad_sequences
from sklearn.model_selection import train_test_split


from pytorch_transformers import XLNetModel, XLNetTokenizer, XLNetForSequenceClassification
from pytorch_transformers import AdamW

from tqdm import tqdm, trange
import pandas as pd
import io
import sys
import numpy as np
import matplotlib.pyplot as plt
# % matplotlib inline

"""In order for torch to use the GPU, we need to identify and specify the GPU as the device. Later, in our training loop, we will load data onto the device."""

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
n_gpu = torch.cuda.device_count()
torch.cuda.get_device_name(0)


path = "/home/kkalouli/Documents/diss/models/sick_trial_train_corrected_xlnet_fine-tuned_model_NEW.pt"
model = XLNetForSequenceClassification.from_pretrained("xlnet-base-cased", num_labels=3)
model.load_state_dict(torch.load(path))
model.cuda()


premise = sys.argv[1]
hypothesis = sys.argv[2]

# Function to calculate the accuracy of our predictions vs labels
def flat_accuracy(preds, labels):
    pred_flat = np.argmax(preds, axis=1).flatten()
    labels_flat = labels.flatten()
    return np.sum(pred_flat == labels_flat) / len(labels_flat)


# We need to add special tokens at the beginning and end of each sentence for XLNet to work properly
sentences = [premise + "[SEP]" + hypothesis + " [SEP] [CLS]" ]
labels = [2]

tokenizer = XLNetTokenizer.from_pretrained('xlnet-base-cased', do_lower_case=True)
tokenized_texts = [tokenizer.tokenize(sent) for sent in sentences]


MAX_LEN = 128

# Use the XLNet tokenizer to convert the tokens to their index numbers in the XLNet vocabulary
input_ids = [tokenizer.convert_tokens_to_ids(x) for x in tokenized_texts]
# Pad our input tokens
input_ids = pad_sequences(input_ids, maxlen=MAX_LEN, dtype="long", truncating="post", padding="post")
# Create attention masks
attention_masks = []
segment_masks = []

# Create a mask of 1s for each token followed by 0s for padding
for seq in input_ids:
  att_mask = [float(i>0) for i in seq]
  attention_masks.append(att_mask) 
  seq_mask = []
  found = False
  for i in seq:
    if i == 0:
      seq_mask.append(1)
    elif i != 23 and found == False:
      seq_mask.append(0)
    elif i == 23 and found == False:
      seq_mask.append(0)
      found = True
    elif i != 23 and found == True:
      seq_mask.append(1)
    elif i == 23 and found == True:
      seq_mask.append(1)
  #print (seq_mask)
  segment_masks.append(seq_mask)

prediction_inputs = torch.tensor(input_ids)
prediction_masks = torch.tensor(attention_masks)
prediction_segment_masks = torch.tensor(segment_masks)
prediction_labels = torch.tensor(labels)
  
batch_size = 32  


prediction_data = TensorDataset(prediction_inputs, prediction_masks, prediction_segment_masks, prediction_labels)
prediction_sampler = SequentialSampler(prediction_data)
prediction_dataloader = DataLoader(prediction_data, sampler=prediction_sampler, batch_size=batch_size)

# Prediction on test set

# Put model in evaluation mode
model.eval()

# Tracking variables 
predictions , true_labels = [], []

nb_test_steps = 0
test_accuracy = 0

# Predict 
for batch in prediction_dataloader:
  # Add batch to GPU
  batch = tuple(t.to(device) for t in batch)
  # Unpack the inputs from our dataloader
  b_input_ids, b_input_mask, b_seg_mask, b_labels = batch
  # Telling the model not to compute or store gradients, saving memory and speeding up prediction
  with torch.no_grad():
    # Forward pass, calculate logit predictions
    outputs = model(b_input_ids, token_type_ids=b_seg_mask, attention_mask=b_input_mask)
    logits = outputs[0]

  # Move logits and labels to CPU
  logits = logits.detach().cpu().numpy()
  label_ids = b_labels.to('cpu').numpy()
  
  # Store predictions and true labels
  predictions.append(logits)
  true_labels.append(label_ids)

  tmp_test_accuracy = flat_accuracy(logits, label_ids)
    
  test_accuracy += tmp_test_accuracy
  nb_test_steps += 1

#print("Test Accuracy: {}".format(test_accuracy/nb_test_steps))

# Flatten the predictions and true values for aggregate Matthew's evaluation on the whole dataset
flat_predictions = [item for sublist in predictions for item in sublist]
flat_predictions = np.argmax(flat_predictions, axis=1).flatten()
#flat_true_labels = [item for sublist in true_labels for item in sublist]

print  (flat_predictions[0])

