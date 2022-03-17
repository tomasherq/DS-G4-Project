import os
import ndjson
from collections import defaultdict
import json


def getIdMessage(idDict):
    ids = list(idDict.values())
    return f'{ids[0]}_{ids[1]}'


def readAdvertisements(directory):

    ads = list()

    for messageFile in os.listdir(directory):
        with open(f'{directory}/{messageFile}', "r") as file_read:

            messages = ndjson.load(file_read)

            for message in messages:

                messageType = list(message["content"].keys())[0]
                if messageType == "advertisement":

                    ads.append(getIdMessage(message["content"]["advertisement"]["ID"]))
    return ads


def readSentAdvertisements(nodeDirectory):

    return readAdvertisements(f'{nodeDirectory}/sent')


def readReceivedAdvertisements(nodeDirectory):

    return readAdvertisements(f'{nodeDirectory}/received')


def checkAdvertisements(receivedAdvertisements, sentAdvertisements):
    missingAds = {}
    for nodeId, receivedAds in receivedAdvertisements.items():
        missingAds[nodeId] = list(set(sentAdvertisements) - set(receivedAds))

    return missingAds


def readAcks(directory):
    acks = defaultdict(list)

    for messageFile in os.listdir(directory):
        with open(f'{directory}/{messageFile}', "r") as file_read:
            messages = ndjson.load(file_read)

            for message in messages:
                messageType = list(message["content"].keys())[0]

                # Is an ACK
                if "messageType" == messageType:

                    sender = message["sender"]["ID"]
                    acks[sender].append(getIdMessage(message["content"]["ID"]))

    return acks


def readSentACKS(nodeDirectory):

    acks = readAcks(f'{nodeDirectory}/sent')
    acksSent = list()
    for valueAcks in acks.values():
        acksSent += valueAcks

    return acksSent


def readReceivedACKS(nodeDirectory):

    return readAcks(nodeDirectory+"/received")
