function downloads() {
  const aid = encodeURIComponent($('#input_aid')[0].value);
  const pid = encodeURIComponent($('#input_pid')[0].value);
  const sid = encodeURIComponent($('#input_sid')[0].value);
  if (aid && pid && sid) {
    $.ajax({
      type: 'GET',
      url: `/downloads?aid=${aid}&pid=${pid}&sid=${sid}`
    })
    .done(msg => {
      if (msg.success) alert("success!")
      else alert("failed!")
    });
  }
}
function get(s) {
  $.ajax({
    type: 'GET',
    url: `/${s}`
  })
  .done(msg => {
    if (msg.success) alert("success!")
    else alert("failed!")
  });
}
function evals() {
  const eid = encodeURIComponent($('#input_eid')[0].value);
  const cid = encodeURIComponent($('#input_cid')[0].value);
  if (eid && cid) {
    $.ajax({
      type: 'GET',
      url: `/evals?eid=${eid}&cid=${cid}`
    })
    .done(msg => {
      if (msg.success) alert("success!")
      else alert("failed!")
    });
  }
}
function pick() {
  const seed = encodeURIComponent($('#input_seed')[0].value);
  const size = $('#input_size')[0].value;
  const gsize = $('#input_gsize')[0].value;
  const num = $('#input_num')[0].value;
  if (seed && size && gsize && num) {
    $.ajax({
      type: 'GET',
      url: `/pick?seed=${seed}&size=${size}&gsize=${gsize}&num=${num}`
    })
    .done(msg => {
      if (msg.success) alert("success!")
      else alert("failed!")
    });
  }
}
