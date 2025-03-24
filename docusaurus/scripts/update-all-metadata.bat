@echo off
REM Update metadata for all Markdown files in the docs directory
echo Updating metadata for all documentation files...

REM Find all Markdown files in the docs directory
dir /b /s ..\docs\*.md > markdown-files.txt

REM Run the metadata update script
node update-metadata.js markdown-files.txt

echo Metadata update complete!
