# MobileVideoCaptureAndUploader
CS5248 Systems Support for Continuous Media Assignment (Task 1: Mobile Video Capture and Uploader)

Task 1 (Mobile Video Capture and Uploader): The mobile application, running on a Samsung tablet computer, is required to provide the following sub-tasks:

1) Capture a live video feed of 720p resultion at 30 fps from the device camera. You can use MediaCodec API from Android to encode frames with h.264 encoding with 5 Mbps bitrate.

2) Upload the captured frames to a web server reliably on-the-fly.
  a. Segment the original live feed into a number of self-contained 3-second-long MP4 segments before uploading on-the-fly. Segmentation can either be done first, before the upload, or in parallel, together with the upload.
  b. Use the HTTP POST method to deliver the segmented MP4 video feed to the server.

For you to understand the internal structure of the MP4 format, we intentationally ask you to segment the video at the mobile client (i.e., at the tablet computer), not at the server. To segment the video, you may use third-party libraries such as MP4Parse.

To upload the segmented video chunks reliably, you are required to design a simple protocol on top of HTTP, such as checking the current upload status or providing segments with a sequence number.

The following are additional functionalities that the mobile app can provide, which will receive extra credits:
  3) Provide a resumed upload when the network connection is interrupted.
  4) Retrieve the list of the uploaded videos available from the web server.
  5) Videos should be playable live onto the client device during a session as well as stored on the server for on-demand playback.
