from functions.readingFunctions import *
from functions.getSizeOfFiles import *
from functions.checkAdvertisements import *
from functions.checkSubscriptions import *
from functions.checkPublications import *

RUNS_DIRECTORY = "../runs/run3"


# I think that the logic is hard to understand

publisherNodes = ["1"]

subscriberNodes = ["4"]

# Measure the traffic per node
totalTraffic = 0
totalTrafficSent = 0
trafficGenerated = {}

# Easiest one cause we do not need to follow paths!
sentAdvertisements = []
sentUnadvertisements = []
receivedAdvertisements = {}
receivedUnadvertisements = {}

# We have to take into account who wants which publication and if it reaches the destinations
sentPublications = []
receivedPublications = {}


# Take into account only if it reached the destination!
sentSubscriptions = {}
sentUnsubscriptions = {}
receivedSubscriptions = {}
receivedUnsubscriptions = {}

# Do we really care about the received ACKs? The important thing is the content
# receivedAcks = {}
# sentAcks = {}

for nodeId in os.listdir(RUNS_DIRECTORY):

    nodeDirectory = RUNS_DIRECTORY+'/'+nodeId

    if nodeId in publisherNodes:

        sentAdvertisements += readSentAdvertisements(nodeDirectory)
        receivedSubscriptions[nodeId] = readReceivedSubscriptions(nodeDirectory)
        receivedUnsubscriptions[nodeId] = readReceivedUnsubscriptions(nodeDirectory)
        sentPublications += readSentPublications(nodeDirectory)
        sentUnadvertisements += readSentUnadvertisements(nodeDirectory)

    elif nodeId in subscriberNodes:

        receivedPublications[nodeId] = readReceivedPublications(nodeDirectory)
        sentSubscriptions[nodeId] = readSentSubscriptions(nodeDirectory)
        sentUnsubscriptions[nodeId] = readSentUnsubscriptions(nodeDirectory)

    else:
        receivedAdvertisements[nodeId] = readReceivedAdvertisements(nodeDirectory)
        receivedUnadvertisements[nodeId] = readReceivedUnadvertisements(nodeDirectory)

    trafficGenerated[nodeId] = getTrafficNode(nodeDirectory)
    totalTraffic += trafficGenerated[nodeId]['total']
    totalTrafficSent += trafficGenerated[nodeId]['sent']
    for key in trafficGenerated[nodeId]:
        trafficGenerated[nodeId][key] = format_bytes(trafficGenerated[nodeId][key])

# Traffic generated
totalTraffic = format_bytes(totalTraffic)
totalTrafficSent = format_bytes(totalTrafficSent)  # Only one that makes sense

# Advertisements
missingAdvertisments = checkAdvertisements(receivedAdvertisements, sentAdvertisements)
missingUnadvertisments = checkAdvertisements(receivedUnadvertisements, sentUnadvertisements)


# Subscriptions
validSubscriptions, potentialSubscriptions = getValidSubscriptions(
    sentSubscriptions, sentAdvertisements, sentUnadvertisements)

unsubscriptionsSummary = getSummaryUnsubscriptions(sentUnsubscriptions)

# Publications
expectedPublications = getExpectedPublications(sentPublications, validSubscriptions, unsubscriptionsSummary)
potentialExpectedPublications = getExpectedPublications(
    sentPublications, potentialSubscriptions, unsubscriptionsSummary)


missingPublications = checkPublications(expectedPublications, receivedPublications)
potentialPublications = checkPublications(potentialSubscriptions, receivedPublications)
