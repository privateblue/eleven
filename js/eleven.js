const players = 1;
const machines = [0];

const colorMap = [[81,134,198], [229,87,16], [190,255,117]];
const names = ['blue', 'red', 'green']
const directionKeyMap = [39, 37, 40, 38];
const directionSymbolMap = ['→', '←', '↓', '↑'];

const width = window.innerWidth;
const height = window.innerHeight;
const size = Math.min(width, height);       // shorter side of screen
const cx = Math.round(width / 2);           // horizontal center of screen
const cy = Math.round(height / 2);          // vertical center of screen
const r = Math.round(size / 25);            // initial radius of value circle
const str = Math.round(r / 3);              // base stroke width factor
const s = Math.round(2.5 * r);              // spacing factor btw value circles
const scoreY = Math.round(1.5 * r);         // score bar vertical position
const fontSize = Math.round(r / 2);         // font size

var circles = [];
var vals = [];
var scrs = [];

var layer;

var player = 0;

initBoard(Game.graphOriginal);
updateBoard(Game.graphOriginal);

var start = Game.start(Game.graphOriginal, players);
setTimeout(() => move(start), 200);

function move(g) {
  var game = Game.gameToJs(g);
  if (game.state == 'continued') {
    // single player moves
    if (players == 1) {
      var rm = Game.randomEmpty(g);
      var random = Game.historyEntryToJs(rm);
      var ep = es => new Promise((res, rej) => res(Game.indexOf(random.put)));
      var dp = pickDirection;
    // machine player moves
    } else if (machines.includes(player)) {
      var bm = Game.bestMove(g);
      var best = Game.historyEntryToJs(bm);
      var ep = es => new Promise((res, rej) => res(Game.indexOf(best.put)));
      var dp = (graph, ds) => new Promise(function(res, rej) {
        updateBoard(graph);
        setTimeout(() => res(Game.directionOf(best.dir)), 1000);
      });
    // human player moves
    } else {
      var ep = pickEmpty;
      var dp = pickDirection;
    }
    var next = Game.move(g, Game.emptyPickerOf(ep), Game.directionPickerOf(dp), Game.resultHandlerOf(handleResult));
    Game.nextToJs(next).then(n => move(n));
  } else if (game.state == 'nomoremoves') {
    var msg = 'no more valid moves, ' + names[game.winner] + ' wins with ' + game.scores[game.winner] + ' points';
    gameOver(msg.toUpperCase());
  } else if (game.state == 'eleven') {
    var msg = names[game.winner] + ' wins with eleven';
    gameOver(msg.toUpperCase());
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

function handleResult(entry, graph, scores) {
  updateBoard(graph);
  updateScores(scores, entry);
  player = (player + 1) % players;
  return new Promise((resolve, reject) => setTimeout(() => resolve(), 200));
}

function updateBoard(g) {
  var graph = Game.graphToJs(g);
  for (i = 0; i < graph.values.length; i++) {
    if ("c" in graph.values[i]) {
      circles[i].radius((1 + graph.values[i].v / 10) * r);
      circles[i].fill(color(colorMap[graph.values[i].c]));
    } else {
      circles[i].radius(r);
      circles[i].fill(color([255,255,255,0]));
    }
    vals[i].setAttr('text', graph.values[i].v);
    vals[i].setOffset({x: vals[i].getWidth() / 2, y: vals[i].getHeight() / 2});
    vals[i].visible(graph.values[i].v != 0);
  }
  layer.draw();
}

function updateScores(ss, he) {
  var scores = Game.scoresToJs(ss);
  var entry = Game.historyEntryToJs(he);
  var scr = scrs[player];
  scr.scr.setAttr('text', '' + scores[player]);
  scr.scr.setOffset({ x: scr.scr.getWidth() / 2, y: 0 });
  if ('dir' in entry) scr.lastm.setAttr('text', directionSymbolMap[entry.dir]);
  scr.lastm.setOffset({ x: scr.lastm.getWidth() / 2, y: 0 });
  scr.gombocka.scale({x: 1, y: 1});
  scrs[(player + 1) % players].gombocka.scale({x: 1.5, y: 1.5});
  layer.draw();
}

function gameOver(msg) {
  scrs[player].gombocka.scale({x: 1, y: 1});
  var message = new Konva.Text({
    x: r + players * 2 * r,
    y: scoreY,
    text: msg,
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  message.setOffset({x: 0, y: message.getHeight() / 2});
  layer.add(message);
  layer.draw();
}

function initBoard(g) {
  var graph = Game.graphToJs(g);

  layer = new Konva.Layer();

  // scores
  var line = new Konva.Line({
    points: [0, scoreY, width, scoreY],
    stroke: 'black',
    strokeWidth: 0.1
  });
  layer.add(line);
  for (i = 0; i < players; i++) {
    var xi = r + i * 2 * r;
    var gombocka = new Konva.Circle({
      name: 'gombocka-' + i,
      x: xi,
      y: scoreY,
      radius: str,
      fill: color(colorMap[i])
    });
    layer.add(gombocka);
    var score = new Konva.Text({
      name: 'score-' + i,
      x:  xi,
      y: scoreY - fontSize - 2 * str,
      text: '',
      fontFamily: 'Dosis',
      fontStyle: 'bold',
      fontSize: fontSize,
      fill: 'black'
    });
    score.setOffset({x: score.getWidth() / 2, y: 0});
    layer.add(score);
    var lastm = new Konva.Text({
      name: 'lastm-' + i,
      x: xi,
      y: scoreY + 2 * str,
      text: '',
      fontFamily: 'Dosis',
      fontStyle: 'bold',
      fontSize: fontSize,
      fill: 'black'
    });
    lastm.setOffset({x: lastm.getWidth() / 2, y: 0});
    scrs.push({gombocka: gombocka, scr: score, lastm: lastm});
    layer.add(lastm);
  }
  var cur = layer.getChildren(c => c.getName() == 'gombocka-0')[0];
  cur.scale({x: 1.5, y: 1.5});

  // circles
  for (y = cy - (3 * s); y <= cy + (3 * s); y = y + s + s) {
    for (x = cx - (3 * s); x <= cx + (3 * s); x = x + s + s) {
      var n = new Konva.Circle({
        x: x,
        y: y,
        radius: str,
        fill: 'black'
      });
      layer.add(n);
      var c = new Konva.Circle({
        x: x,
        y: y,
        radius: r,
        fill: color([255,255,255,0])
      });
      circles.push(c);
      layer.add(c);
      var t = new Konva.Text({
        x: x,
        y: y,
        text: '0',
        fontFamily: 'Dosis',
        fontStyle: 'bold',
        fontSize: 1.5 * fontSize
      });
      t.setOffset({x: t.getWidth() / 2, y: t.getHeight() / 2});
      t.visible(false);
      vals.push(t);
      layer.add(t);
    }
  }

  // arrows
  for (d = 0; d < graph.edges.length; d++) {
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
        pointerLength: 0,
        pointerWidth: 0,
        fill: 'black',
        stroke: 'black',
        strokeWidth: 0.5
      });
      layer.add(a);
      a.setZIndex(0);
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
    // up
    return [x1, y1 - str, x2, y2 + str];
  } else if (x1 === x2 && y2 > y1) {
    // down
    return [x1, y1 + str, x2, y2 - str];
  } else if (y1 === y2 && x1 > x2) {
    // left
    return [x1 - str, y1, x2 + str, y2];
  } else if (y1 === y2 && x2 > x1) {
    // right
    return [x1 + str, y1, x2 - str, y2];
  }
}

function color(arr) {
  if (arr.length > 3) {
    return 'rgba(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ',' + arr[3] + ')';
  } else {
    return 'rgb(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ')';
  }
}
