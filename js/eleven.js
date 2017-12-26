var players = 3;

var colorMap = [[0,0,255], [255,0,0], [0,255,0]];
var directionKeyMap = [39, 37, 40, 38];
var circles = [];
var indicators = [];
var arrows = [];
var vals = [];
var scrs = [];
var player;

initBoard(players, Game.graphOriginal);
updateBoard(Game.graphOriginal);

var start = Game.start(Game.graphOriginal, players);
move(start);

function move(g) {
  var game = Game.gameToJs(g);
  player = game.move % players;
  if (game.state == 'continued') {
    var emptyPicker = Game.emptyPickerOf(pickEmpty);
    var directionPicker = Game.directionPickerOf(pickDirection);
    var resultHandler = Game.resultHandlerOf(handleResult);
    //var next = Game.move(g, emptyPicker, directionPicker, resultHandler);
    var next = Game.moveWithRandomEmpty(g, directionPicker, resultHandler);
    Game.nextToJs(next).then(move);
  } else if (game.state == 'nomoremoves') {
    console.log('no more valid moves');
  } else if (game.state == 'eleven') {
    console.log('eleven!');
  }
}

function pickEmpty(empties) {
  var es = Game.emptiesToJs(empties);
  return new Promise(function(resolve, reject) {
    for (i = 0; i < es.length; i++) {
      circles[es[i]].on("click", function(evt) {
        circles.forEach(c => c.off("click"));
        var index = circles.findIndex(c => c == this);
        resolve(Game.indexOf(index));
      });
    }
  });
}

function pickDirection(graph, directions) {
  updateBoard(graph);
  var dirs = Game.directionsToJs(directions);
  var keys = dirs.map(d => directionKeyMap[d]);
  return new Promise(function(resolve, reject) {
    document.addEventListener('keydown', function _handler(evt) {
      evt.preventDefault();
      if (keys.includes(evt.keyCode)) {
        document.removeEventListener('keydown', _handler, false);
        var direction = directionKeyMap.indexOf(evt.keyCode);
        resolve(Game.directionOf(direction));
      }
    }, false);
  });
}

function handleResult(graph, scores) {
  updateBoard(graph);
  var ss = Game.scoresToJs(scores);
  for (i = 0; i < ss.length; i++) {
    scrs[i].bkg.draw();
    if (('' + ss[i]).length > scrs[i].scr.getAttr('text').length) {
      scrs[i].scr.fontSize(Math.round(scrs[i].scr.fontSize() / 5 * 4));
    }
    scrs[i].scr.setAttr('text', '' + ss[i]);
    scrs[i].scr.setOffset({x: scrs[i].scr.getWidth() / 2, y: scrs[i].scr.getHeight() / 2});
    scrs[i].scr.draw();
  }
  return new Promise((resolve, reject) => resolve());
}

function updateBoard(g) {
  indicators.forEach(t => t.reset());
  var graph = Game.graphToJs(g);
  for (i = 0; i < graph.values.length; i++) {
    if ("c" in graph.values[i]) {
      var c = colorMap[graph.values[i].c].slice();
      if (graph.values[i].v != 0) c.push(graph.values[i].v / 10);
      circles[i].fill(color(c));
      circles[i].draw();
    } else {
      circles[i].fill('white');
      circles[i].draw();
    }
    indicators[i] = new Konva.Tween({
      node: circles[i],
      scaleX: 1.2,
      scaleY: 1.2,
      duration: 0.1,
      onFinish: function() { this.reverse(); }
    });
    if ((!vals[i].visible() && graph.values[i].v != 0) || graph.values[i].v > parseInt(vals[i].getAttr('text'))) {
      indicators[i].play();
    }
    vals[i].setAttr('text', graph.values[i].v);
    vals[i].setOffset({x: vals[i].getWidth() / 2, y: vals[i].getHeight() / 2});
    vals[i].visible(graph.values[i].v != 0);
    vals[i].draw();
  }
}

function initBoard(p, g) {
  var graph = Game.graphToJs(g);

  var width = window.innerWidth;
  var height = window.innerHeight;
  var size = Math.min(width, height);

  var cx = Math.round(width / 2);
  var cy = Math.round(height / 2);
  var r = Math.round(size / 15);
  var str = Math.round(r / 10);
  var s = Math.round(r * 3 / 2);
  var scrw = Math.round((6 * s + 2 * r) / (p + 1));
  var scrr = Math.round(r / 2);
  var scrr2 = Math.round(scrr / 3 * 4);

  var layer = new Konva.Layer();

  // scores
  for (i = 0; i < p; i++) {
    var star = new Konva.Star({
      x: cx - (3 * s) - r + (i + 1) * scrw,
      y: cy - (3 * s) - 4 * scrr,
      numPoints: 7,
      innerRadius: scrr,
      outerRadius: scrr2,
      fill: color(colorMap[i])
    });
    var scr = new Konva.Text({
      x: cx - (3 * s) - r + (i + 1) * scrw,
      y: cy - (3 * s) - 4 * scrr,
      text: '0',
      fontFamily: 'Bungee',
      fontSize: scrr,
      fill: 'white'
    });
    scr.setOffset({x: scr.getWidth() / 2, y: scr.getHeight() / 2});
    scrs.push({bkg: star, scr: scr});
    layer.add(star);
    layer.add(scr);
  }
  var rotate = new Konva.Animation(function(frame) {
    scrs[player].bkg.rotate(frame.timeDiff / 10);
  }, layer);
  rotate.start();

  // circles
  for (y = cy - (3 * s); y <= cy + (3 * s); y = y + s + s) {
    for (x = cx - (3 * s); x <= cx + (3 * s); x = x + s + s) {
      var c = new Konva.Circle({
        x: x,
        y: y,
        radius: r,
        stroke: 'black',
        strokeWidth: str
      });
      circles.push(c);
      layer.add(c);
      var t = new Konva.Text({
        x: x,
        y: y,
        text: '0',
        fontFamily: 'Bungee',
        fontSize: r
      });
      t.setOffset({x: t.getWidth() / 2, y: t.getHeight() / 2});
      t.visible(false);
      vals.push(t);
      layer.add(t);
    }
  }

  // arrows
  for (d = 0; d < graph.edges.length; d++) {
    arrows.push([]);
    for (e = 0; e < graph.edges[d].length; e++) {
      var edge = graph.edges[d][e];
      var from = circles[edge.from];
      var to = circles[edge.to];
      var x1 = from.attrs.x;
      var y1 = from.attrs.y;
      var x2 = to.attrs.x;
      var y2 = to.attrs.y;
      var a = new Konva.Arrow({
        points: arrowPoints(x1, y1, x2, y2, r, str),
        pointerLength: str,
        pointerWidth : str,
        fill: 'black',
        stroke: 'black',
        strokeWidth: str / 2
      });
      arrows[d].push(a);
      layer.add(a);
    }
  }

  var stage = new Konva.Stage({
    container: 'container',
    width: width,
    height: height
  });
  stage.add(layer);
}

function arrowPoints(x1, y1, x2, y2, r, str) {
  if (x1 === x2 && y1 > y2) {
    // north
    return [x1, y1 - r, x2, y2 + r + str];
  } else if (x1 === x2 && y2 > y1) {
    // south
    return [x1, y1 + r, x2, y2 - r - str];
  } else if (y1 === y2 && x1 > x2) {
    // east
    return [x1 - r, y1, x2 + r + str, y2];
  } else if (y1 === y2 && x2 > x1) {
    // west
    return [x1 + r, y1, x2 - r - str, y2];
  }
}

function color(arr) {
  if (arr.length > 3) {
    return 'rgba(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ',' + arr[3] + ')';
  } else {
    return 'rgb(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ')';
  }
}
