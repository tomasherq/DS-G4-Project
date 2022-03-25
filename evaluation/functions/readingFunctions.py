import os
import ndjson
from collections import defaultdict
import json
from functions.checkAdvertisements import getIdMessage


def readByMessageType(directory, messageType):
    messagesRead = list()
    for messageFile in os.listdir(directory):
        with open(f'{directory}/{messageFile}', "r") as file_read:

            messages = ndjson.load(file_read)

            for message in messages:

                keyTypeMessage = list(message["content"].keys())[0]

                if messageType == keyTypeMessage:

                    objectMessage = message["content"][messageType]
                    objectMessage["timestamp"] = message["timestamp"]
                    messagesRead.append(message["content"][messageType])
    return messagesRead

# Advertisements


def readSentAdvertisements(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/sent', "advertisement")


def readReceivedAdvertisements(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/received', "advertisement")

# Unadvertisements


def readSentUnadvertisements(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/sent', "unadvertisement")


def readReceivedUnadvertisements(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/received', "unadvertisement")


# Publications

def readSentPublications(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/sent', "publication")


def readReceivedPublications(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/received', "publication")

# Subscriptions


def readSentSubscriptions(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/sent', "subscription")


def readReceivedSubscriptions(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/received', "subscription")


# Unsubscriptions
def readSentUnsubscriptions(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/sent', "unsubscription")


def readReceivedUnsubscriptions(nodeDirectory):
    return readByMessageType(f'{nodeDirectory}/received', "unsubscription")


# Acks


def readTimeoutACK(directory):
    acks = defaultdict(list)
    directory = f'{directory}/received'
    retransIds = list()
    for messageFile in os.listdir(directory):
        with open(f'{directory}/{messageFile}', "r") as file_read:
            messages = ndjson.load(file_read)

            for message in messages:
                messageType = list(message['content'].keys())[0]

                # Is an ACK
                if "messageType" == messageType:
                    if "Publish" in message["content"]["messageType"]:
                        retransIds.append(getIdMessage(message["content"]['ID']))

    return retransIds


# def readSentACKS(nodeDirectory):

#     acks = readAcks(f'{nodeDirectory}/sent')
#     acksSent = list()
#     for valueAcks in acks.values():
#         acksSent += valueAcks

#     return acksSent


# def readReceivedACKS(nodeDirectory):

#     return readAcks(nodeDirectory+"/received")
