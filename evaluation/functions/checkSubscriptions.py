from collections import defaultdict
from functions.checkAdvertisements import getIdMessage


def getAnnuncedSubscriptions(sentSubscriptions, sentAdvertisements):
    validSubscriptions = defaultdict(list)
    potentialSubscriptions = defaultdict(list)
    for nodeId in sentSubscriptions:

        subscriptionsOfNode = sentSubscriptions[nodeId]

        for subscription in subscriptionsOfNode:
            valid = False
            for advertisement in sentAdvertisements:

                if subscription["pClass"] == advertisement["pClass"]:
                    operation = advertisement["pAttributes"]["_1"]
                    if operation == subscription["pAttributes"]["_1"] or operation == "ne":

                        adValue = advertisement["pAttributes"]["_2"]
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

                        if valid and subscription not in validSubscriptions[nodeId]:
                            if subscription["timestamp"] > advertisement["timestamp"]:
                                validSubscriptions[nodeId].append(subscription)
                                break
                            else:
                                potentialSubscriptions[nodeId].append(subscription)
                                break

    return validSubscriptions, potentialSubscriptions


def cleanSubscriptions(validSubscriptions, sentUnadvertisements):

    for nodeId in validSubscriptions:
        subscriptionsOfNode = validSubscriptions[nodeId]
        for subscription in subscriptionsOfNode:
            valid = False
            for advertisement in sentUnadvertisements:

                if subscription["timestamp"] > advertisement["timestamp"]:
                    if subscription["pClass"] == advertisement["pClass"]:
                        operation = advertisement["pAttributes"]["_1"]
                        if operation == subscription["pAttributes"]["_1"] or operation == "ne":

                            adValue = advertisement["pAttributes"]["_2"]
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

                            if valid and subscription in validSubscriptions[nodeId]:
                                validSubscriptions[nodeId].remove(subscription)
                                break
    return validSubscriptions


def getValidSubscriptions(sentSubscriptions, sentAdvertisements, sentUnadvertisements):

    validSubscriptions, potentialSubscriptions = getAnnuncedSubscriptions(sentSubscriptions, sentAdvertisements)

    validSubsClean = cleanSubscriptions(validSubscriptions, sentUnadvertisements)
    potentialSubsClean = cleanSubscriptions(potentialSubscriptions, sentUnadvertisements)

    return validSubsClean, potentialSubsClean


def getSummaryUnsubscriptions(sentUnsubscriptions):
    # I am only interested in the IDs and the timestamp of the unsubs

    unsubscriptions = defaultdict(lambda: {})

    for nodeId in sentUnsubscriptions:
        for sentUnsub in sentUnsubscriptions[nodeId]:
            unsubscriptions[nodeId][getIdMessage(sentUnsub["ID"])] = sentUnsub["timestamp"]

    return unsubscriptions
