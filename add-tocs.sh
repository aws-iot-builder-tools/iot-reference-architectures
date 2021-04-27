#!/usr/bin/env bash

# Requires markdown-toc -- npm install -g markdown-toc
sudo npm install -g markdown-toc
# Requires rename -- brew install rename
brew install rename
find . -name "*.without-toc.md" -exec sh -c "cp {} {}-processed.md && markdown-toc -i {}-processed.md && rename -f 's/without-toc.md-processed.md/md/' '{}-processed.md'" \;
