from functions.readingFunctions import *
from functions.getSizeOfFiles import *
from functions.checkAdvertisements import *
from functions.checkSubscriptions import *
from functions.checkPublications import *

RUNS_DIRECTORY = "../runs/run3"

# Get retransmissions and timeouts from ACKs.


def getSumOfField(dictionary, field):
    result = 0
    for nodeId in dictionary:
        result += dictionary[nodeId][field]
    return result


# I think that the logic is hard to understand
publisherNodes = ["1"]

subscriberNodes = ["4"]

# Measure the traffic per node
trafficGenerated = {}
trafficGenerated["totalSent"] = 0

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
    trafficGenerated["totalSent"] += trafficGenerated[nodeId]['sent']
    for key in trafficGenerated[nodeId]:
        trafficGenerated[nodeId][key] = format_bytes(trafficGenerated[nodeId][key])


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


subscriberStats = checkPublications(expectedPublications, receivedPublications)
potentialPublications = checkPotentialPublications(expectedPublications, potentialExpectedPublications)


# Creation of the summary
summary = {}
summary['n_nodes'] = len(os.listdir(RUNS_DIRECTORY))
summary['n_publishers'] = len(publisherNodes)
summary['n_subscribers'] = len(subscriberNodes)
summary['n_brokers'] = summary['n_nodes'] - len(publisherNodes) - len(subscriberNodes)
summary['stats'] = {}
summary['stats']['avg_wait_time'] = getSumOfField(subscriberStats, "waitTime")/summary['n_subscribers']
summary['stats']['traffic_sent'] = format_bytes(trafficGenerated["totalSent"])
summary['stats']['traffic_sent_bytes'] = trafficGenerated["totalSent"]
summary['stats']['avg_traffic_sent'] = format_bytes(trafficGenerated["totalSent"]/summary['n_nodes'])
summary['stats']['recv_pubs'] = getSumOfField(subscriberStats, "receivedPubs")/summary['n_subscribers']
summary['stats']['avg_recv_pubs'] = getSumOfField(subscriberStats, "receivedPubs")/summary['n_subscribers']
summary['stats']['miss_pubs'] = getSumOfField(subscriberStats, "missingPubs")
summary['stats']['avg_miss_pubs'] = getSumOfField(subscriberStats, "missingPubs")/summary['n_subscribers']
summary['stats']['avg_miss_rate'] = getSumOfField(subscriberStats, "missRate")/summary['n_subscribers']
summary['stats']['pot_pubs_miss'] = getSumOfField(
    potentialPublications, "potentialPubs")/summary['n_subscribers']
summary['stats']['pot_pubs_miss_rate'] = getSumOfField(
    potentialPublications, "potentialPubsRate")/summary['n_subscribers']


runName = RUNS_DIRECTORY.split("/")[-1]

with open(f'results/{runName}.json', 'w') as file_write:
    file_write.write(json.dumps(summary, indent=4))


# Interesting elements:
# totalTraficSent
# missingPublications
# potential publications
# averageWaiting time and average waits
