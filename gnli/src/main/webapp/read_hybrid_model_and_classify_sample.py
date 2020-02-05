import pickle, sys
import numpy as np
import pandas as pd


# load the model from disk
filename = '/home/kkalouli/Documents/diss/models/hybrid_model_trained_on_sick_and_mccoy_xlnet.sav'
loaded_model = pickle.load(open(filename, 'rb'))

# get console input
input = sys.argv[1]
bert_label = sys.argv[2]
rule_label = sys.argv[3]

# convert input features to an appropriate array for prediction
input_as_list = input.strip('\[\]').split(',')
#print (input_as_list)
features = np.asarray([int(x) for x in input_as_list])
#print (features)
feat_reshaped = features.reshape(1, -1)
#print (feat_reshaped)
prediction = loaded_model.predict(feat_reshaped)
#print(prediction)

feature_importances = pd.DataFrame(loaded_model.feature_importances_,  index=["complexCtxs","contraFlag","VER","ANTIVER","AVER","eq","super","sub","dis"],columns=['importance']).sort_values('importance', ascending=False)

# decide on final label
if prediction == "B":
	label = bert_label
elif prediction == "R":
	label = rule_label
elif prediction == "BR":
	label = bert_label
else:
	label = rule_label

label = label.replace("0", "E").replace("1", "C").replace("2", "N")
weighted_feats = list(feature_importances.index.values)

weighted_feats.append(label)

print(weighted_feats)