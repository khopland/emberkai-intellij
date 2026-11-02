#!/usr/bin/env bash
set -euo pipefail

tag="v$VERSION"

# Listing releases distinguishes an absent release from authentication/API failures.
releases=$(gh api "repos/$GITHUB_REPOSITORY/releases" --paginate | jq -s 'add')
release=$(jq -c --arg tag "$tag" '[.[] | select(.tag_name == $tag)] | sort_by(.draft) | first // empty' <<< "$releases")
if [[ -n "$release" && $(jq -r '.draft' <<< "$release") == false ]]; then
  echo "Version $VERSION is already released; increment pluginVersion and add its changelog entry for the next release."
  exit 0
fi

# Never move a published tag or attach new binaries to a tag for another commit.
remote_tag=$(git ls-remote origin "refs/tags/$tag" "refs/tags/$tag^{}")
if [[ -n "$remote_tag" ]]; then
  tag_sha=$(awk '/\^\{\}$/ { peeled=$1 } { direct=$1 } END { print peeled ? peeled : direct }' <<< "$remote_tag")
  if [[ "$tag_sha" != "$GITHUB_SHA" ]]; then
    echo "Tag $tag already points to another commit; leaving its release unchanged."
    exit 0
  fi
fi

mkdir -p build/release-notes
notes="build/release-notes/$VERSION.md"
awk -v header="## [$VERSION]" '
  /^## / { if (found) exit; if (index($0, header) == 1) { found = 1; next } }
  found && !/^\[[^]]*\]: / { print }
' CHANGELOG.md | sed -e '/./,$!d' | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}' > "$notes"
if [[ ! -s "$notes" ]]; then
  echo "::error::CHANGELOG.md has no entries under '## [$VERSION]'"
  exit 1
fi

(cd build/distributions && sha256sum ./*.zip > SHA256SUMS)
prerelease=false
if [[ "$VERSION" == *-* ]]; then prerelease=true; fi
if [[ -n "$release" ]]; then
  gh release edit "$tag" --draft --target "$GITHUB_SHA" --title "Emberkai $VERSION" --notes-file "$notes" --prerelease="$prerelease"
else
  gh release create "$tag" --draft --target "$GITHUB_SHA" --title "Emberkai $VERSION" --notes-file "$notes" --prerelease="$prerelease"
fi
gh release upload "$tag" build/distributions/*.zip build/distributions/SHA256SUMS --clobber
echo "Prepared $tag from tested commit $GITHUB_SHA. Publish this draft to start Marketplace delivery."
