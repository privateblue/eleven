var colorMap = ['red', 'blue', 'yellow'];
var directionKeyMap = [39, 37, 40, 38];
var circles = [];
var arrows = [];
var texts = [];

initGraph(Game.graphOriginal);
updateGraph(Game.graphOriginal);

var players = 2

var start = Game.start(Game.graphOriginal, players);
move(start);

function move(g) {
  var game = Game.gameToJs(g);
  if (game.state == 'continued') {
    var emptyPicker = Game.emptyPickerOf(pickEmpty);
    var directionPicker = Game.directionPickerOf(pickDirection);
    var resultHandler = Game.resultHandlerOf(handleResult);
    var next = Game.move(g, emptyPicker, directionPicker, resultHandler);
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
  updateGraph(graph);
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
  updateGraph(graph);
  return new Promise((resolve, reject) => resolve());
}

function updateGraph(g) {
  var graph = Game.graphToJs(g);
  for (i = 0; i < graph.values.length; i++) {
    if ("c" in graph.values[i]) {
      var color = colorMap[graph.values[i].c];
      circles[i].fill(color);
      circles[i].draw();
    } else {
      circles[i].fill('white');
      circles[i].draw();
    }
    if (graph.values[i].v != 0) {
      texts[i].setAttr('text', graph.values[i].v);
      texts[i].moveToTop();
      texts[i].visible(true);
      texts[i].draw();
    } else {
      texts[i].visible(false);
      texts[i].draw();
    }
  }
}

function initGraph(g) {
  var graph = Game.graphToJs(g);

  var width = window.innerWidth;
  var height = window.innerHeight;
  var size = Math.min(width, height);

  var cx = Math.round(width / 2);
  var cy = Math.round(height / 2);
  var r = Math.round(size / 15);
  var str = Math.round(r / 10);
  var s = Math.round(r * 3 / 2);

  var layer = new Konva.Layer();

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
         fontFamily: 'monospace',
         fontSize: 50
       });
       t.setOffset({x: t.getWidth() / 2, y: t.getHeight() / 2});
       t.visible(false);
       texts.push(t);
       layer.add(t);
    }
  }

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
