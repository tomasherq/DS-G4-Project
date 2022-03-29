from functions.readingFunctions import *
from functions.getSizeOfFiles import *
from functions.checkAdvertisements import *
from functions.checkSubscriptions import *
from functions.checkPublications import *

RUNS_DIRECTORY = "/Users/remyduijsens/Documents/Education/Master Computer Science/Q3/Distributed Systems/Project/DS-G4-Project/runs/run7"

# Get retransmissions and timeouts from ACKs.


def getSumOfField(dictionary, field):
    result = 0

    for nodeId in dictionary:
        if field in dictionary[nodeId]:
            result += dictionary[nodeId][field]
    if isinstance(result, float):
        result = round(result, 2)

    return result


# I think that the logic is hard to understand
publisherNodes = ["13", "14"]

subscriberNodes = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"]

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
retransPublications = []

# Take into account only if it reached the destination!
sentSubscriptions = {}
sentUnsubscriptions = {}
receivedSubscriptions = {}
receivedUnsubscriptions = {}

# Do we really care about the received ACKs? The important thing is the content
# receivedAcks = {}
# sentAcks = {}

for nodeId in os.listdir(RUNS_DIRECTORY):

    if "root" in nodeId:
        continue
    nodeDirectory = RUNS_DIRECTORY+'/'+nodeId

    if nodeId in publisherNodes:

        sentAdvertisements += readSentAdvertisements(nodeDirectory)
        receivedSubscriptions[nodeId] = readReceivedSubscriptions(nodeDirectory)
        receivedUnsubscriptions[nodeId] = readReceivedUnsubscriptions(nodeDirectory)
        sentPublications += readSentPublications(nodeDirectory)
        sentUnadvertisements += readSentUnadvertisements(nodeDirectory)
        retransPublications += readTimeoutACK(nodeDirectory)

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


subscribersEmpty = {}
for node in subscriberNodes:
    subscribersEmpty[node] = {}

subscriberStats = checkPublications(expectedPublications, receivedPublications, retransPublications, subscribersEmpty)
potentialPublications = checkPotentialPublications(expectedPublications, potentialExpectedPublications)


# Creation of the summary
numberSubs = len(subscriberNodes)

summary = {}
summary['n_nodes'] = len(os.listdir(RUNS_DIRECTORY))
summary['n_publishers'] = len(publisherNodes)
summary['n_subscribers'] = numberSubs
summary['n_brokers'] = summary['n_nodes'] - len(publisherNodes) - len(subscriberNodes)
summary['stats'] = {}
summary['stats']['avg_wait_time'] = getSumOfField(subscriberStats, "waitTime")/numberSubs
summary['stats']['traffic_sent'] = format_bytes(trafficGenerated["totalSent"])
summary['stats']['traffic_sent_bytes'] = trafficGenerated["totalSent"]
summary['stats']['avg_traffic_sent'] = format_bytes(trafficGenerated["totalSent"]/summary['n_nodes'])
summary['stats']['recv_pubs'] = getSumOfField(subscriberStats, "receivedPubs")
summary['stats']['avg_recv_pubs'] = getSumOfField(subscriberStats, "receivedPubs")/numberSubs
summary['stats']['miss_pubs'] = getSumOfField(subscriberStats, "missingPubs")
summary['stats']['avg_miss_pubs'] = getSumOfField(subscriberStats, "missingPubs")
summary['stats']['avg_miss_rate'] = getSumOfField(subscriberStats, "missRate")/numberSubs
summary['stats']['avg_pot_pubs_miss'] = getSumOfField(potentialPublications, "potentialPubs")/numberSubs
summary['stats']['avg_pot_pubs_miss_rate'] = getSumOfField(potentialPublications, "potentialPubsRate")/numberSubs
summary['stats']['avg_rtr_pubs'] = getSumOfField(subscriberStats, "totalRetrans")/numberSubs
summary['stats']['avg_success_rtr_pubs'] = getSumOfField(subscriberStats, "succesRetrans")/numberSubs

avg_recv_pubs = summary['stats']['avg_recv_pubs']
if avg_recv_pubs < 1:
    avg_recv_pubs = 1

summary['stats']['avg_rtr_pubs_to_normal'] = round(
    summary['stats']['avg_success_rtr_pubs']/avg_recv_pubs, 2)

runName = RUNS_DIRECTORY.split("/")[-1]

with open(f'/Users/remyduijsens/Documents/Education/Master Computer Science/Q3/Distributed Systems/Project/DS-G4-Project/evaluation/results/{runName}.json', 'w') as file_write:
    file_write.write(json.dumps(summary, indent=4))

with open(f'/Users/remyduijsens/Documents/Education/Master Computer Science/Q3/Distributed Systems/Project/DS-G4-Project/evaluation/results/{runName}_per_node.json', 'w') as file_write:
    file_write.write(json.dumps(subscriberStats, indent=4))

# Interesting elements:
# totalTraficSent
# missingPublications
# potential publications
# averageWaiting time and average waits
