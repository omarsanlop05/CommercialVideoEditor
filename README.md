# Commercial Video Editor Project

## Description
This program is a commercial video editor and creator. It allows the user to create a basic commercial using AI given some multimedia files (videos and photos) and a brief description of the mood of the commercial video.

## Features
- **Image Creation Using AI**: The program creates two postcards using the given mood description prompt. One is used at the beginning and other for closing.
- **Video Merging**: The program reprocesses all the given files, both videos and photos, to be videos of the same format and codec using ffmpeg. The program respects the aspect ratio and orientation fo the files, but scales all of them to 1920x1080 resolution. It avoids stretching the files, adding black border filter if the original aspect ratio is not 16:9.
- **Chronological Order**: The program uses exiftool to extract the metadata dates and selects the older one to arrange the order in which the files will be shown in the final output video.
- **AI voice narration**: The program uses AI to make a description of the files the images the user gives. In the case of the video, it uses it's thumbnail to create the description. This description is later narrated using AI voice when the videos are shown. The program calculates the amount of words needed to the description to fit with the length of the video.
- **cURL Commands**: The program uses cURL to make the API calls necessary for the AI created content. (Except for image description, the API only accepts url or base64 images, and cURL had limited characters, so it was used http requests.)
- **Collage**: The program uses ffmpeg to create a collage of all the given files by the user. Its shown after all the videos and before the final postcard.

## Usage

1. **Enter files**: Enter the files desired to make the commercial video from the UI clicking on "Select Files" button to open File Manager. The user must enter at least 3 files, else the program will not activate "Generate Video" button. The File Manager uses a filter to only accept valid files (with video or image extension).
2. **Enter a brief description**: Use the text field to make a brief description of the mood you want for the video. It will be later used to make the initial and final postcards.
3. **Generate Video**: If the prompt field is filled and selected at least 3 files, the "Generate Video" button will activate. Click it to start generating the video. The UI will freeze until the video has been generated, you can see the ongoing process in the console for more details. At the end of the process, console will show the full route where the video was saved.
4. **Final Output**: During the process, two directories will be generated. "output-files" are the one used during the process of creating the video. There you will find files as the postcards, final audio, joined videos, final video (without audio), collage, etc. "final-output-video" will have only the final result of the program.

# Requirements

If any of these requirements is not fulfilled, the program will not work correctly.

- **Windows**: The program is designed to work in Microsoft Windows OS.
- **API Key**: Must enter **YOUR** personal API Key on the variable of the "ChatGPT" class file.
- **cURL**: Must have cURL correctly installed on the device. To check it, use "curl --version" on CMD.
- **ffmpeg.exe**: Must have ffmpeg on the route of the project ".../CommercialVideoEditor/ffmpeg.exe".
- **ffprobe.exe**: Must have ffprobe on the route of the project ".../CommercialVideoEditor/ffmprobe.exe".
- **exiftool.exe**: Must have extracted latest version of exif tool (exiftool-13.25_64) on route of the project. Change name of the .exe file from "exiftool(-k).exe" to "exiftool.exe". The final route must be ".../CommercialVideoEditor/exiftool-13.25_64/exiftool.exe".
- **Entered file route**: If the path to the entered files by the user has a directory that uses spaces " " on its name, windows may not recognize it correctly.
- **Internet Connection**: Internet connection is required to make the API calls.


# Details

Programmed in OpenJDK2023.

By Omar Alejandro Sánchez López (0262155), Universidad Panamericana Campus Guadalajara


