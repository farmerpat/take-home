$(document).ready(function () {
  localStorage.repos = JSON.stringify([]);
  localStorage.current_release_notes_repo_name = '';

  $("#repo_refresh_button").click(function (e) {
    console.log("repo_refresh_button clicked");
    // get the list of current repo names
    // send them to /api and get latest releases for all
    // for each latest release that is more recent that current:
    //   put new_repo class on its repo_entry_container
    //   update the date
    // blow out displayed release_notes if they have been updated.
  });

  function set_repo_entry_click_events() {
    $(".repo_entry").click(function (e) {
      var repo_name = $(this).find(".repo_name_container").text().trim().split('/')[1] || '';
      if (!(localStorage.current_release_notes_repo_name == repo_name)) {
        var repo = get_from_repo_cache(repo_name);
        set_release_notes(repo.release_notes);
        localStorage.current_release_notes_repo_name = repo_name;
      }

      $(this).removeClass('new_repo');
      $('.repo_entry').removeClass('selected_repo');
      $(this).addClass('selected_repo');
    });

    $(".repo_remove_container").click(function (e) {
      console.log("repo_remove_container clicked");
      var repo_name = $(this).parent().find(".repo_name_container").text().trim().split('/')[1] || '';
      if (repo_name != '') {
        if (localStorage.current_release_notes_repo_name == repo_name) {
          $("#repo_release_notes_container").html('');
        }

        $(this).parent().remove();
        delete_from_repo_cache(repo_name);
      }
    });
  }

  $('input#repo_input').keypress(function (event) {
    if (event.which === 32) {
      return false;
    } else if (event.which === 13) {
      $("#repo_search_submit").click();
    }
  });

  function get_cache() {
    return JSON.parse(localStorage.repos);
  }

  function save_cache(new_cache) {
    localStorage.repos = JSON.stringify(new_cache);
  }

  function search_repo_cache(repo_name, cache) {
    cache = cache || get_cache();
    var found = false;
    $(cache).each(function (i, elt) {
      if (elt.name == repo_name) {
        found = true;
        return false;
      }
    });

    return found;
  }

  function delete_from_repo_cache(repo_name, cache) {
    cache = cache || get_cache();
    var new_cache = cache.filter(function (repo) { return repo.name != repo_name; });
    save_cache(new_cache);
  }

  function get_from_repo_cache(repo_name, cache) {
    cache = cache || get_cache();
    var repo = null;

    $(cache).each(function (i, elt) {
      if (elt.name == repo_name) {
        repo = elt;
        return false;
      }
    });

    return repo;
  }

  function add_repo_to_repo_cache(repo, cache) {
    cache = cache || get_cache();
    cache.push(repo);
    save_cache(cache);
  }

  function update_repo_in_repo_cache(repo, cache) {
    cache = cache || get_cache();
    if (search_repo_cache(repo.name, cache)) {
      delete_from_repo_cache(repo.name);
    }

    add_repo_to_repo_cache(repo, cache);
  }

  function update_repo_cache(repo) {
    var name = repo.name;
    var current_repos = get_cache();

    if (search_repo_cache(name, current_repos)) {
      update_repo_in_repo_cache(repo);
    } else {
      add_repo_to_repo_cache(repo);
    }
  }

  function format_date_string(ds) {
    return new Date(ds).toLocaleString(
      'en-US',
      {year:'numeric', day: 'numeric', month:'numeric'
    });
  }

  function set_release_notes(rn) {
    $("#repo_release_notes_container").html(rn);
  }

  // TODO:
  // This fn should not create the html, it should use it...
  // e.g. pass it in!
  function update_repo_list(repo) {
    if (! (repo.release_date && repo.release_notes)) {
      return;
    }

    // This should probably happen in the back-end,
    // but where's the best place for it?
    // another route to be called from FE?
    // in the api route itself?
    var html = `
<div class="repo_entry_container">
        <div class="repo_entry new_repo">
            <div class="repo_name_container">
                ${repo.owner + '/' + repo.name}
            </div>
            <div class="repo_time_and_action_container">
                <div class="repo_time_container">
                    ${format_date_string(repo.release_date)}
                </div>
            </div>
        </div>
        <div class="repo_remove_container">X</div>
</div>`;

    $("#repos_container").prepend(html);
    set_repo_entry_click_events();
  }

  function perform_repo_search (repo_name, callback) {
    callback = callback || function(d) {};

    //var repo_name = $("#repo_input").val();

    if (repo_name != '') {
      $.ajax({
        method: "get",
        url: "/api/repo/search",
        contentType: "application/json",
        dataType: "json",
        data: {
          repo_name: repo_name
        },
        success: function (search_data) {
          var repo;

          if (search_data && search_data.body && search_data.body.data) {
            repo = search_data.body.data;

            // TODO:
            // probably combine these into one api call
            $.ajax({
              method: "get",
              url: "/api/repo/release",
              contentType: "json",
              dataType: "json",
              data: {
                repo: repo

              },
              success: function (release_data) {
                release_data = release_data && release_data.body && release_data.body.data;
                if (release_data && release_data.release_notes && release_data.release_notes) {
                  callback(Object.assign(repo, release_data));

                }
              },
              error:function (jqxhr, textStatus, error) {
                console.log("erra, erra: " + error);

              }
            });
          }
        },
        error: function (jqxhr, textStatus, error) {
          console.log("erra: " + error);

        }
      });
    }
  }

  $("#repo_search_form").submit(false);

  $("#repo_search_submit").click(function () {
    var repo_name = $("#repo_input").val().trim();

    if (search_repo_cache(repo_name)) {
      // TODO:
      // may want to do something (e.g. notify user)
    } else {
      perform_repo_search(repo_name, function (repo) {
        update_repo_cache(repo);
        update_repo_list(repo);
        $("#repo_input").val('');
      });
    }
  });
});

