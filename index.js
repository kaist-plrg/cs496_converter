function get(s) {
  const id = encodeURIComponent($('#input_id')[0].value);
  if (id) {
    $(".loader").css("display", "block");
    $.ajax({
      type: 'GET',
      url: `/${s}?id=${id}`
    })
    .done(msg => {
      $(".loader").css("display", "none");
      if (!msg.success) alert("failed!");
    });
  } else
    alert("empty id");
}
function pick() {
  const seed = encodeURIComponent($('#input_seed')[0].value);
  const size = $('#input_size')[0].value;
  const gsize = $('#input_gsize')[0].value;
  const num = $('#input_num')[0].value;
  const id = encodeURIComponent($('#input_id')[0].value);
  if (seed && size && gsize && num && id) {
    $(".loader").css("display", "block");
    $.ajax({
      type: 'GET',
      url: `/pick?seed=${seed}&size=${size}&gsize=${gsize}&num=${num}&id=${id}`
    })
    .done(msg => {
      $(".loader").css("display", "none");
      if (msg.success) {
        if ($("#div-result")[0]) $("#div-result")[0].remove();
        $("body").append('<div class=".table-wrapper" id="div-result"></div>');
        const div = $("#div-result");
        for (const t of msg.result) {
          div.append(`<h2>${t[0]}</h2>`);
          drawTable(div, t[1]);
        }
      } else alert("failed!");
    });
  } else
    alert("empty id");
}
var x = 0;
function drawTable(div, rs) {
  div.append(`<table id="table-${x}"></table>`);
  const table = $(`#table-${x}`);
  for (const r of rs)
    table.append(
      '<tr>' + r.map(x => `<td>${x}</td>`).join('\n') + '</tr>'
    );
  x += 1;
}
