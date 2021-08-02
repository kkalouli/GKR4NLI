# Gets features, the symbolic label and the DL labels and predicts the label of the pretrained hybrid classifier.
import pickle, sys
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier


# Load the pretrained hybrid model. Modify this line to point to the location where you downloaded the model.
filename = '/home/kkalouli/Documents/diss/models/hybrid_model_for_HyNLI.sav'
loaded_model = pickle.load(open(filename, 'rb'))

# Get console input: the features (of the GKR4NLI system), the BERT label and the symbolic label
input = sys.argv[1]
bert_label = sys.argv[2]
rule_label = sys.argv[3]

# Convert input features to an appropriate array for prediction
input_as_list = input.strip('\[\]').split(',')
#print (input_as_list)
features = np.asarray([int(x) for x in input_as_list])
#print (features)
feat_reshaped = features.reshape(1, -1)
#print (feat_reshaped)

# Predict the hybrid label.
prediction = loaded_model.predict(feat_reshaped)
#print(prediction)


# Decide on final label, based on which component was predicted to assign the correct inference label. Note
# that here you use the "old" abbreviations, where "B" stands for BERT, "R" for rule-based and "BR" for hybrid (BERT and rule-based)
if prediction == "B":
	label = bert_label
elif prediction == "R":
	label = rule_label
elif prediction == "BR":
	label = bert_label
else:
	label = rule_label

# Convert the numeric labels to the common inference labels.
label = label.replace("0", "E").replace("1", "C").replace("2", "N")
print (label)