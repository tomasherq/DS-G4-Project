

from collections import defaultdict
from functions.checkAdvertisements import getIdMessage


def getExpectedPublications(sentPublications, validSubscriptions, unsubscriptionsSummary):

    expectedPublications = defaultdict(list)

    for nodeId, subscriptions in validSubscriptions.items():

        copiedPublications = sentPublications.copy()

        for subscription in subscriptions:

            for publication in copiedPublications:

                subscriptionID = getIdMessage(subscription["ID"])

                if subscriptionID in unsubscriptionsSummary[nodeId]:
                    timestampUnsub = unsubscriptionsSummary[nodeId][subscriptionID]
                    if timestampUnsub < publication["timestamp"]:
                        continue

                if subscription["timestamp"] > publication["timestamp"]:

                    if subscription["pClass"] == publication["pClass"]:
                        operation = publication["pAttributes"]["_1"]
                        if operation == subscription["pAttributes"]["_1"] or operation == "ne":

                            adValue = publication["pAttributes"]["_2"]
                            subValue = subscription["pAttributes"]["_2"]

                            if operation == "gt":
                                if(adValue >= subValue):
                                    valid = True
                            elif operation == "lt":
                                if(adValue <= subValue):
                                    valid = True
                            elif operation == "e":
                                if(adValue == subValue):
                                    valid = True
                            if subscription["pAttributes"]["_1"] == "ne":
                                valid = valid and adValue != subValue

                            if valid and publication not in expectedPublications[nodeId]:
                                expectedPublications[nodeId].append(publication)
                                copiedPublications.remove(publication)
                                break
        return expectedPublications


def checkPublications(expectedPublications, receivedPublications):

    missingPubs = {}

    def getIdsPublications(publications):

        ids = []
        for publication in publications:
            if publication['ID'] not in ids:
                ids.append(getIdMessage(publication['ID']))
        return ids
    for nodeId in expectedPublications:
        if nodeId in receivedPublications:

            expectedIds = getIdsPublications(expectedPublications[nodeId])
            receivedIds = getIdsPublications(receivedPublications[nodeId])
            missingPubs[nodeId] = list(set(expectedIds) - set(receivedIds))

    return missingPubs
