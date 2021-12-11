$(document).ready(function () {
  $('input#repo_input').keypress(function (event) {
    if (event.which === 32) {
      return false;
    } else if (event.which === 13) {
      perform_repo_search(update_repo_list);
    }
  });

  function update_repo_list(repo) {
    console.log("i r update_repo_list");
    console.log(repo);
  }

  function perform_repo_search (callback) {
    callback = callback || function(d) {};

    var repo_name = $("#repo_input").val();

    if (repo_name != '') {
      $.ajax({
        method: "get",
        url: "/api/repo/search",
        contentType: "application/json",
        dataType: "json",
        data: {
          repo_name: repo_name
        },
        success: function (data) {
          console.log(data);

          if (data && data.body && data.body.data) {
            var repo = data.body.data;
            console.log(repo);
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
              success: function (data) {
                console.log(data);
                callback(data);
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
    perform_repo_search(update_repo_list);
  });
});

