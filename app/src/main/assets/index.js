var myApp = {};
myApp.globals = {};

//function update_location(dir) {
//  if(dir)
//    location.hash = encodeURIComponent(dir);
//  else
//    location.hash = '';
//}
//
//$(window).on('hashchange', function(e){
//  if(location.hash.length > 0)
//    retrieve_listing(location.hash.substring(1));
//  else
//    retrieve_listing(null);
//});

function retrieve_listing(dir) {

    var query_params = '';
    if(dir) {
      query_params = '?dir=' + dir;
    }

    $.ajax({

       url: '/list' + query_params,
       type: 'GET',
       dataType: 'json',

       success: function(json) {

         var dir_changed = !myApp.globals.dir || myApp.globals.dir !== json.dir;
         myApp.globals.dir = json.dir;

         $('#breadcrumbs_div').html(breadcrumbs(json.dir));
         populate_favorites();

         $('#listing_table tr').has('td').remove();
         $('#select_all').prop('checked', false);

         $(function() {
             $.each(json.entries, function(i, entry) {

                 var span = $('<span/>');

                 if(entry.isDir) {

                   $('<img/>', {
                       src: 'folder-icon.png',
                       style: 'width:20px;height:20px' 
                   }).appendTo(span);
                   span.append(' ');

                   // var a = $('<a/>', { href: '#' });
                   var a = $('<a/>');
                   a.addClass('link');
                   a.html(entry.name);
                   a.on('click',
                      { path: entry.path },
                      function(eventObject) {
                          retrieve_listing(eventObject.data.path);
                      });
                   a.appendTo(span);

                 } else if(entry.isImage) {

                    var a = $('<a/>', {
                      href: '/view?file=' + entry.path,
                      title: entry.name
                    });
                    var img = $('<img />', {
                        src: '/thumbnail?file=' + entry.path,
                        alt: entry.name
                    });
                    img.appendTo(a);
                    a.appendTo(span);

                 } else {
                    var a = $('<a/>', { href: '/view?file=' + entry.path })
                    a.html(entry.name);
                    a.appendTo(span);
                 }
                 
                 var dl_a = $('<a/>', {
                     html: '<img src="arrow-download-icon.png" style="width:20px;height:20px"/>',
                     href: '/download?path=' + entry.path,
                     title: entry.isDir ? 'Download as Zip' : 'Download'
                 });

                 var rm_a = $('<a/>', {
                     html: '<img src="delete.ico" style="width:20px;height:20px" title="Delete"/>',
                     href: '#'
                 });
                 rm_a.on('click',
                   { dir: json.dir, path: entry.path },
                   function(eventObject) {
                     delete_path(eventObject.data.dir, eventObject.data.path);
                   });

                 var ckbox = $('<input/>', {
                   id: 'ckbox_' + i,
                   type: 'checkbox',
                   value: json.dir + '/' + entry.name 
                 });

                 $('<tr>').append(
                     $('<td>').html(ckbox),
                     $('<td>').html(span).addClass(entry.isImage ? 'center' : ''),
                     $('<td>').text(human_readable_size(entry.size)),
                     $('<td>').text(entry.lastModified),
                     $('<td>').html(dl_a).addClass('center'),
                     $('<td>').html(rm_a).addClass('center')
                 ).appendTo('#listing_table');
             });
         });    

         if(dir_changed) {
           init_file_upload(json.dir);  
           $('#fileList').empty();   
         }

         // If the directory is writable, show the file upload components, otherwise hide them.
         if(json.writable) {
           $('#drop-area-div-container').show();
           $('#fileList').show();
           $('#del_selected_btn').show();
         } else {
           $('#drop-area-div-container').hide();
           $('#fileList').hide();
           $('#del_selected_btn').hide();
         }
       },

       error: function(xhr, status, errorThrown) {
         handle_error('Failed to retrieve directory listing', xhr, status, errorThrown);
       }
    });
}

function init_file_upload(dir) {

  $('#drop-area-div').remove();
  var drop_area_div = $('<div/>');
  drop_area_div.attr('id', 'drop-area-div');
  var html = 'Drop to Upload, or <input type="file" name="files[]" multiple="multiple" title="Click to add Files">';
  drop_area_div.html(html);
  // drop_area_div.addClass('uploader');
  drop_area_div.appendTo($('#drop-area-div-container'));

  drop_area_div.dmUploader({

    url: '/upload?dir=' + dir,

    onInit: function() {
      console.log('File upload successfully initialized');
    },

    onFallbackMode: function(message) {
      alert('File uploader plugin can\'t be initialized');
    },

    onNewFile: function(id, file) {
      // Fields available in file object: file.name, file.type, file.size
      console.log('File ' + file.name + ' added to queue');
      add_file(id, file);
    },

    onBeforeUpload: function(id){
      console.log('Starting to upload #' + id);
      update_file_status(id, 'uploading', 'Uploading...');
    },

    onComplete: function(){
      console.log('We reach the end of the upload Queue!');
    },

    onUploadProgress: function(id, percent){
      // console.log('Upload of #' + id + ' is at ' + percent + '%');
      update_file_progress(id, percent + '%');
    },

    onUploadSuccess: function(id, data){
      console.log('Successfully upload #' + id);
      console.log('Server response was:');
      console.log(data);
      update_file_status(id, 'success', 'Upload Complete');
      update_file_progress(id, '100%');      
      retrieve_listing(dir);
    },

    onUploadError: function(id, message){
      console.log('Error trying to upload #' + id + ': ' + message);
      update_file_status(id, 'error', message);
      // alert('Failed to upload file: ' + message);
    }
  });
}

function add_file(id, file)
{
  var template = '' +
    '<div class="file" id="uploadFile' + id + '">' +
      '<div class="info">' +
        '#1 - <span class="filename" title="Size: ' + file.size + 'bytes - Mimetype: ' + file.type + '">' + file.name + '</span><br /><small>Status: <span class="status">Waiting</span></small>' +
      '</div>' +
      '<div class="bar">' +
        '<div class="progress" style="width:0%"></div>' +
      '</div>' +
    '</div>';
    
    $('#fileList').prepend(template);
}

function update_file_status(id, status, message)
{
  $('#uploadFile' + id).find('span.status').html(message).addClass(status);
}

function update_file_progress(id, percent)
{
  $('#uploadFile' + id).find('div.progress').width(percent);
}

function breadcrumbs(dir) {
  
  var breadcrumbs_div = $('<div/>');

  // The root / directory is a special case.  If we just split it normally we'll get
  // ['', ''].
  if(dir === '/') {
    var path_components = [''];
  } else {
    var path_components = dir.split('/');
  }

  // After splitting on slash, we'll lose the leading slash and have an empty
  // string in the array at position zero.
  for(var i = 0; i < path_components.length; i++) {
    
    if(i == 0) {
      var path = '/';
      var label = '/';
    } else {
      var path = path_components.slice(0, i + 1).join('/');
      var label = path_components[i];
    }

    var anchor = $('<a/>', {
      // href: '#',
      html: label
    });
    anchor.addClass('link');
    anchor.on('click',
      {path: path},
      function(eventObject) {
        retrieve_listing(eventObject.data.path);
      });

    anchor.appendTo(breadcrumbs_div);
    if(i < path_components.length - 1) {
      if(i == 0)
        breadcrumbs_div.append('  ')
      else
        breadcrumbs_div.append(' / ');
    }
  }

  return breadcrumbs_div;
}

function populate_favorites() {
    $.ajax({

       url: '/favorites',
       type: 'GET',
       dataType: 'json',

       success: function(json) {
         if(json.length > 0) {
             favorites_div = $('<div/>');
             $(function() {
                 $.each(json, function(i, favorite) {

                    var anchor = $('<a/>', {
                        html: favorite.dir
                    });
                    anchor.addClass('link');
                    anchor.on('click',
                      { dir: favorite.dir },
                      function(eventObject) {
                        retrieve_listing(eventObject.data.dir);
                      });
                    anchor.appendTo(favorites_div);

                    var rm_a = $('<a/>', {
                        html: '<img src="delete.ico" style="width:12px;height:12px" title="Delete"/>',
                        href: '#'
                    });
                    rm_a.on('click',
                      { dir: favorite.dir },
                      function(eventObject) {
                        delete_favorite(eventObject.data.dir);
                      });
                    rm_a.appendTo(favorites_div);

                    $('<br />').appendTo(favorites_div);
                 });
             });
             $('#favorites_div').html(favorites_div);

//             $('#favorites_div').show();
             $('#favorites_dropdown').show();
         } else {
             $('#favorites_div').hide();
             $('#favorites_dropdown').hide();
         }
       },

       error: function(xhr, status, errorThrown) {
         handle_error('Failed to retrieve favorites', xhr, status, errorThrown);
       }
    });
}

function human_readable_size(bytes) {

  if(bytes) {
    var kB = 1024;
    var MB = 1024 * kB;
    var GB = 1024 * MB;

    if(bytes < kB) {
      return bytes + ' B';
    } else if(bytes < MB) {
      return format(bytes / kB, 1) + ' kB';
    } else if(bytes < GB) {
      return format(bytes / MB, 1) + ' MB';
    } else {
      return format(bytes / GB, 1) + ' GB';
    }
  } else {
    return '-';
  }
}

function format(f, places) {
  var pow_ten = Math.pow(10, places);
  return parseFloat(Math.round(f * pow_ten) / pow_ten).toFixed(places);  
}

function delete_path(dir, path) {

  if(confirm('Delete ' + path + '?')) {
    $.ajax({

      url: '/?path=' + path,
      type: 'DELETE',

      success: function() {
        retrieve_listing(dir);
      },

      error: function(xhr, status, errorThrown) {
        handle_error('Failed to delete path', xhr, status, errorThrown);
      }
    });
  }
}

function delete_favorite(dir) {
    $.ajax({

      url: '/?favorite=' + dir,
      type: 'DELETE',

      success: function() {
        populate_favorites();
      },

      error: function(xhr, status, errorThrown) {
        handle_error('Failed to delete favorite', xhr, status, errorThrown);
      }
    });
}

// $(document).ajaxSuccess(function(event, request, settings) {
//   if(request.getResponseHeader('REQUIRES_AUTH') === '1') {
//     window.location.href = '/login.html';
//   }
// });


$(document).ready(function(){
//  update_location(null);
  retrieve_listing(null);

  $('#title_span').on('click',
    { },
    function(eventObject) {
      retrieve_listing(null);
    });

  $('#select_all').on('click',
    { },
    function(eventObject) {
      $('#container input').each(function() {
        if($(this).attr('id') !== 'select_all') {
          var currently_checked = $('#select_all').is(':checked');
          $(this).prop('checked', currently_checked);
        }
      });
    });

  $('#dwnl_selected_btn').on('click',
    {  },
    function(eventObject) {
      if(count_selected() > 0) {
        window.location.href = '/download' + multiselect_query_string();
      } else {
        alert('Please select at least one item to download.');
      }
    });

  $('#del_selected_btn').on('click',
    {  },
    function(eventObject) {
      var count = count_selected();
      if(count > 0) {
        if(confirm('Delete ' + count + ' items?')) {
          $.ajax({

            url: '/' + multiselect_query_string(),
            type: 'DELETE',

            success: function() {
              retrieve_listing(myApp.globals.dir);
            },

            error: function(xhr, status, errorThrown) {
              handle_error('Failed to delete path(s)', xhr, status, errorThrown);
            }
          });
        }
      } else {
        alert('Please select at least one item to delete.');
      }
    });

  $('#favorites_dropdown').on('click',
    {  },
    function(eventObject) {
      $('#favorites_div').toggle('fast');
      var fav_arrow = $('#favorites_arrow');
      var img_src = fav_arrow.attr('src') === 'right-arrow.png' ? 'down-arrow.png' : 'right-arrow.png';
      $('#favorites_arrow').attr('src', img_src);
    });
});

function count_selected() {
  var count = 0;
  $('#container input:checked').each(function() {
    count++;  
  });
  return count;
}

function multiselect_query_string() {
  var query = '?';
  $('#container input:checked').each(function() {
    if($(this).attr('id') !== 'select_all') {
      if(query.length > 1)
        query += '&';
      query += 'path=';
      query += $(this).attr('value'); 
    }
  });

  return query;
}

function handle_error(msg, xhr, status, errorThrown) {
  // This is totally a hack.  If authentication fails, the server will
  // return a 301 redirect to the login page.  But the browser handles this
  // transparently rather than just passing it along to the AJAX handler.  So
  // the result is that the AJAX handler gets the html of the login page, and
  // attempts to parse that as JSON, which of course fails producing the 'parsererror'
  // here.  Haven't found a better way to handle this yet.  
  if(status === 'parsererror') {
    window.location.href = '/login.html';
  } else {
    alert(msg);
    console.log('Error: ' + errorThrown);
    console.log('Status: ' + status);
    console.dir(xhr);
  }
}

