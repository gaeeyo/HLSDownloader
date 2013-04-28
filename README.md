HLSDownloader
=============

HLS の m3u8 に記述されたファイルをダウンロードして1つのファイルにする

`adb am start -a android.intent.action.VIEW -t application/x-mpegURL -d http://example.com/playlist.m3u8`
`adb am start -a android.intent.action.VIEW -t application/vnd.apple.mpegurl -d http://example.com/playlist.m3u8`

