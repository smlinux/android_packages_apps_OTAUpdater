# Android OTA updater
An Over the Air updater for Android based operating systems.

## Features
- Automatically check for updates
- Show changelog for updates
- Download and install updates
- Check MD5 sum after download
- Patch system for incremental updates

## Setup
First of all you need a JSON file containing all required information about the updates on a public server. The URL to this file has to be set in strings.xml as `builds_list_uri`.

The JSON file has a JSON array as root element. Whenever releasing an update you have to upload the file to a public server and add a JSON object at the first position of this array so it looks like this:
```json
[
  {
    "name": "v1.1",
    "filename": "systemupdate-20160811.zip",
    "md5": "ce21cd30d997e0fe35f280ee5ec98c7b",
    "size": 317902317,
    "builddate": 1470934360,
    "releasedate": 1470938828000,
    "device": "m0",
    "url": "https://mydownloadserver.org/file/12345678",
    "patchlevel": 0,
    "changelog": [
      "A bug fix",
      "Another bug fix"
    ]
  }
]
```
The latest build is the first item of the array.
Each update JSON object has to have the following attributes:

`name`: The name of the update which is visible for the user

`filename`: The filename which is used to save the update file on the device

`md5`: the MD5 checksum of the file.

`size`: the size of the update file in bytes

`builddate`: the build date of the build, has to be equal to `ro.build.date.utc` in the build.prop of the ROM, is required to ensure that an update is compatible.

`releasedate`: the date when the update was released, release time and date in millis (will be shown in updater, you can use `$ echo $(stat -c %Y updatefile.zip)000` to get it)

`device`: the device the update is determined for. Has to be equal to `ro.product.device` in build.prop

`url`: a direct link to the update file

`patchlevel`: 0 for full updates, >=1 for incemental updates (see below for more information)

`changelog`: a JSON Array of strings containing all the changelog entries shown in the updater app

### Releasing an full update
To release a full build (which contains the entire system) upload your build file to a public server where it can be accessed per direct link (so without clicking a download button or something like this) and add a new JSON element to your JSON as described above.

### Providing patches
The OTAUpdater supports providing incremental updates (called patches). Every patch belongs to exactly one build.
#### First patch of a build
To create a patch zip file you can use the patch.zip which is included in this repository. Copy all files which need to change in this update to the system directory of the zip. Keep the directory structure! In addition to all changed files you have to copy the build.prop from the original build (which the patch belongs to) to system/build.prop in the zip. Add `ro.build.patchlevel=1` somewhere in the build.prop. Now upload your patch file somewhere and add it to your JSON file. In the JSON do everything like descriped above except that you write `"patchlevel": 1`. Use the same build date as in the build since this is how the OTAUpdater recognizes to which build the patch belongs. Patches (with patchlevel>0) will only be shown in the updater app if
- the builddate of the patch is the equal to the build date of the installed build
- the patchlevel of the patch is greater than the patchlevel of the system

#### Patches after the first patch
If you have a build which already has a patch and you want to create a second/third/fourth/... patch use the patch file from the previous build as a base. Now unpack the build.prop from the zip and increase the number at `ro.build.patchlevel` by one. Copy all files which changed in this patch to the archive but do not remove any file (if which already is in the archive has changed again overwrite it). This ensures that users can upgrade directly from patch 1 to 3 or whatever without having to flash all previous patches. Again upload the patch file and add it to your JSON. Use the same patch level in the JSON as you used in build.prop.

## License
```
Copyright 2016 MaxMustermann2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
