#!/bin/sh
cd bin 
java main.RunPeer 1 1 peer1 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446
java main.RunPeer 1 2 peer2 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446   
java main.RunPeer 1 3 peer3 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446  
java main.RunPeer 1 4 peer4 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446 
java main.RunPeer 1 5 peer5 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446 
