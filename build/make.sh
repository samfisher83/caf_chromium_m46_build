#!/bin/bash

LOCAL_REPO="$(dirname $(dirname $(readlink -f $0)))"
ROMName=$(basename $LOCAL_REPO)

# parameter "--system" allows not to start the build immdeiately
# parameter "--no-gclient" allows not to run "gclient runhooks -v" - to check the pervious commits logs
isCustom="$1"

cd $LOCAL_REPO/src

# this is because they placed WebRefiner and WebDefender translation into zip archive >_<
pushd $LOCAL_REPO/src/components/web_refiner/java/
  cp -rf $LOCAL_REPO/build/webrefiner/values-ru .
  zip -0TX libswewebrefiner_java.zip values-ru/strings.xml
  rm -rf values-ru/
  git add -f $(git status -s | awk '{print $2}') && git commit -m "Adding WebRefiner and WebDefender translation"
popd

# this commit is here because "gclient sync -n --no-nag-max" changes some files and they need to be either reset or committed to make repo clean
git add -f $(git status -s | awk '{print $2}') && git commit -m "Dummy"

# revert "Disable edge navigation in low-power mode" - I want it! :)
git revert 5b20b729a33916a315e03f2be7be8edcca7bd27e

# reverting Google sign-in and extended bookmarks related removals
# (well, they are not removed but placed under ENABLE_SUPPRESSED_CHROMIUM_FEATURES flag, but this flag is not added for actual usage)
# some of them are part of other commits, so had to use patching
git apply $LOCAL_REPO/build/patches/signin.patch && git add -f $(git status -s | awk '{print $2}') && git commit -m "Getting sign-in back"

# I do not know other way to get it themed, sorry
git apply $LOCAL_REPO/build/patches/themes.patch && git add -f $(git status -s | awk '{print $2}') && git commit -m "Masking to Chrome Beta for themes support :->"

# removing Google Translate tick as it does not work anyway
git apply $LOCAL_REPO/build/patches/remove_translate.patch && git add -f $(git status -s | awk '{print $2}') && git commit -m "Remove page translation tick"

cp -f $LOCAL_REPO/build/webrefiner/web_refiner_conf $LOCAL_REPO/src/chrome/android/java/res_chromium/raw/
git add -f $(git status -s | awk '{print $2}') && git commit -m "Shamelessly stealing WebRefiner config from JSwarts and extending it"

# reverting to old bookmarks UI - have to change strategy due to 9fd8eb1f1374a51f048ec255f8e341ff2e381234
git apply $LOCAL_REPO/build/patches/bookmarks.patch && git add -f $(git status -s | awk '{print $2}') && git commit -m "Reverting to old bookmarks UI"
git revert 2f8a15af8865836a98c578138dc7f59e1b043cf7 || git add -f $(git status -s | awk '{print $2}') && git revert --continue

# media files saving and exit dialog are disabled by default
git apply $LOCAL_REPO/build/patches/qrd_features.patch && git add -f $(git status -s | awk '{print $2}') && git commit -m "Enable QRD features"

git apply $LOCAL_REPO/build/patches/swe_strings.patch && git add -f $(git status -s | awk '{print $2}') && git commit -m "Adding SWE translations"

if [[ "$isCustom" != "--no-gclient" ]];
then

  gclient runhooks -v

  # implementing custom translated lines build
  patch -p0 < $LOCAL_REPO/build/patches/chrome_strings_grd_ninja.diff

else
  exit 0
fi

# for ROM build env - to allow it starting system package build itself
if [[ "$isCustom" != "--system" ]];
then

  $LOCAL_REPO/build/run.sh &

fi
