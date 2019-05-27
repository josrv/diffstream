## Demonstration

1. Launch stream  
`java ServerKt 9000`  
2. Connect as a host and specify host editor file (it should exist)  
`java ClientKt localhost 9000 host host.txt`  
3. Connect as a viewer and specify viewer editor file (it should exist)  
`java ClientKt localhost 9000 viewer viewer.txt`  
4. Type anything in host.txt and watch viewer.txt getting updated.  
