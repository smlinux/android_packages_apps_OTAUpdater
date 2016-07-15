# Android OTA updater
An Over the Air updater for Android based operating systems.

## Features
- Automatically check for updates
- Show changelog for updates
- Download and install updates
- Check MD5 sum after download
- Patch system for incremental updates

## Setup
First of all you need a JSON file containing all required information about the updates. The URL to this file has to be set in strings.xml as `builds_list_uri`.

The JSON file has to be a JSON Array which look like this:
```json
[
  {
    "name": "July 2016 Update",
    "filename": "systemupdate-20160715.zip",
    "md5": "cf4d435e1f767593fa323e62803ddd65",
    "size": 317902317,
    "builddate": 1468538300,
    "releasedate": 1467412175000,
    "device": "m0",
    "url": "https://mydownloadserver.org/file/121af6182",
    "patchlevel": 0,
    "changelog": [
      "A really cool new feature"
    ]
  }
]
```
The latest build is the first item of the array.
Each update JSON item has to have the following attributes:

`name`: The name of the update which is visible for the user

`filename`: The filename which is used to save the update file on the device

`md5`: the MD5 checksum of the file (you can use `$ md5sum updatefile` to get it)

`size`: the size of the update file in bytes

`builddate`: the build date of the build, has to be equal to `ro.build.date.utc` in the build.prop of the ROM

`releasedate`: the date when the update was released, release time and date in millis (you can use `$ echo $(stat -c %Y updatefile.zip)000` to get it)

`device`: the device the update is determined for. Has to be the same as `ro.product.device` in build.prop

`url`: a direct link to the update file

`patchlevel`: 0 for full updates, 1 or higher for incemental updates (see below for more information)

`changelog`: a JSON Array of strings containing all the changelog entries

# To be continued... Readme is WIP
