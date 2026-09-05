# Media Server

This is a possible concept for Xenon 2.0 or later. 

## Media Server and Resource Type

I was thinking about media types one night and how they are internally 
distinguished today simply by the scheme. This could be enhanced or more 
formally defined by the concept of a media server where the media server
takes the place of the scheme. One particular responsibility of the media 
server, if possible, is to determine the media type for the resource.

## Resource Category
This also got me thinking about the two main categories of resource types:

- Connection resources
- Stream resources

Where stream resources are also a type of connection resource.