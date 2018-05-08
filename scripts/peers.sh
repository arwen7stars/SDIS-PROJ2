#!/bin/bash

#initialize normal peers
xterm -e "cd bin && java main.RunPeer 1.0 1 peer1 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446" &  
xterm -e "cd bin && java main.RunPeer 1.0 2 peer2 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446" & 
xterm -e "cd bin && java main.RunPeer 1.0 3 peer3 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446" & 
xterm -e "cd bin && java main.RunPeer 1.0 4 peer4 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446" &  
xterm -e "cd bin && java main.RunPeer 1.0 5 peer5 224.0.0.255 4446 225.0.0.0  4446 225.0.0.1 4446"
