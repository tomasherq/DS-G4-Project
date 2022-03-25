from collections import defaultdict


def getIdMessage(idDict):
    ids = list(idDict.values())
    return f'{ids[0]}_{ids[1]}'


def checkAdvertisements(receivedAdvertisements, sentAdvertisements):
    missingAds = {}
    sentAds = list()
    for sentAd in sentAdvertisements:
        sentAds.append(getIdMessage(sentAd["ID"]))

    for nodeId, receivedAds in receivedAdvertisements.items():
        ids_received = list()
        for receivedAd in receivedAds:
            ids_received.append(getIdMessage(receivedAd["ID"]))

        missingAds[nodeId] = list(set(sentAds) - set(ids_received))

    return missingAds
