# take-home

The take-home exercise.
It's a Luminus app with jQuery covering the front end
which was described by the following US:

1. Users can add GitHub repos they want to keep track of.   App keeps track of added repos (e.g. using LocalStorage/IndexedDB/or DB for API) until application data is cleared.
   1. For a Web/Mobile only project, no need for a formal user or authentication concept at this point.
   2. For an API project, consider how you would authenticate and authorize users to access the github API and store relevant repo data.
2. Users can see the last release date associated with each repo.
3. Users can mark a release as seen (for example, this can be done in the sample UI below by opening the details view associated with each repo)
4. There is a visual indicator for repositories with new releases since they were last marked as seen.
5. There is a way to reload release data for all repos (e.g. by refreshing the app)

Typing the name of a repository in the text field, "Repo" and clicking "Add"
will perform a search for a repository with that name and the first result
(if it exists) will be added to the list of repositories.
Unseen repositories have a green background and clicking a repository in the list
will mark it as seen and display its release notes in the div beside the repo list.
Clicking the "X" next to a repository in the list will remove it from the list and
clear the release notes if applicable.
The "Refresh Repos" button asks for the latest release for each listed repo and the intent
is that results with newer release dates are reflected in the UI as per the AC.
It may work, but the only tests I've run so far were for stale releases.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

```bash
$ lein run 
```

Visit localhost:3000 in a web browser.

## TODO
- [ ] More end to end testing
- [ ] More unit tests
- [ ] Front-End MVP
    - [x] Allow repository removal
    - [ ] Allow repository refresh
      - [ ] But actually test it
- [ ] Deal with > 60 requests per hour
- [ ] Refactor backend where applicable?
- [ ] Clean up
