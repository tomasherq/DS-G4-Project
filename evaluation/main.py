from functions.countMessages import *
from functions.getSizeOfFiles import *

RUNS_DIRECTORY = "../runs/run1"


publisherNodes = ["1"]

subscriberNodes = []

# Measure the traffic per node
totalTraffic = 0
totalTrafficSent = 0
trafficGenerated = {}

# Easiest one cause we do not need to follow paths!
sentAdvertisements = []
receivedAdvertisements = {}

# We have to take into account who wants which publication and if it reaches the destinations
sentPublications = []
receivedPublications = {}


# Take into account only if it reached the destination!
sentSubscriptions = {}
receivedSubscriptions = {}

# Do we really care about the received ACKs? The important thing is the content
# receivedAcks = {}
# sentAcks = {}

for nodeId in os.listdir(RUNS_DIRECTORY):

    nodeDirectory = RUNS_DIRECTORY+'/'+nodeId

    if nodeId in publisherNodes:
        # We only need a list of advertisements
        sentAdvertisements += readSentAdvertisements(nodeDirectory)

    elif nodeId in subscriberNodes:
        pass
    else:
        receivedAdvertisements[nodeId] = readReceivedAdvertisements(nodeDirectory)

    trafficGenerated[nodeId] = getTrafficNode(nodeDirectory)
    totalTraffic += trafficGenerated[nodeId]['total']
    totalTrafficSent += trafficGenerated[nodeId]['sent']
    for key in trafficGenerated[nodeId]:
        trafficGenerated[nodeId][key] = format_bytes(trafficGenerated[nodeId][key])

totalTraffic = format_bytes(totalTraffic)
totalTrafficSent = format_bytes(totalTrafficSent)

missingAdvertisments = checkAdvertisements(receivedAdvertisements, sentAdvertisements)

print(json.dumps(missingAdvertisments, indent=4))
print(json.dumps(trafficGenerated, indent=4))
print(totalTrafficSent)
