#!/bin/bash

echo "Write arguments to initialize the client (<peer_ap> <sub_protocol> <opnd_1> <opnd_2>)"

read arguments

cd bin && java client.Client $arguments
