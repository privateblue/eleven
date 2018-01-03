const players = 1;
const machines = [];

const colorMap = [[0,0,255], [255,0,0], [0,255,0]];
const directionKeyMap = [39, 37, 40, 38];
const directionSymbolMap = ['→', '←', '↓', '↑'];

const width = window.innerWidth;
const height = window.innerHeight;
const size = Math.min(width, height);                     // shorter side of screen
const cx = Math.round(width / 2);                         // horizontal center of screen
const cy = Math.round(height / 2);                        // vertical center of screen
const r = Math.round(size / 25);                          // initial radius of value circle
const str = Math.round(r / 3);                            // base stroke width factor
const s = Math.round(2.5 * r);                            // spacing factor btw value circles
const scoreY = Math.round(1.5 * r);                       // score bar vertical position
const scoreW = Math.round(width / players / 2 - 2 * str); // spacing of score bar elements
const fontSize = Math.round(r / 2);                       // font size

var circles = [];
var wobbles = [];
var vals = [];

var scoreBar;
var layer;

var player = 0;
var n = 0;

initBoard(Game.graphOriginal);
updateBoard(Game.graphOriginal);

var start = Game.start(Game.graphOriginal, players);
move(start);

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
        setTimeout(() => res(Game.directionOf(best.dir)), 500);
      });
    // human player moves
    } else {
      var ep = pickEmpty;
      var dp = pickDirection;
    }
    var next = Game.move(g, Game.emptyPickerOf(ep), Game.directionPickerOf(dp), Game.resultHandlerOf(handleResult));
    Game.nextToJs(next).then(n => move(n));
  } else if (game.state == 'nomoremoves') {
    gameOver('NO MORE VALID MOVES, PLAYER ' + game.winner + ' WINS WITH ' + game.scores[game.winner] + ' POINTS');
  } else if (game.state == 'eleven') {
    gameOver('PLAYER ' + game.winner + ' WINS WITH ELEVEN');
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
  n = n + 1;
  return new Promise((resolve, reject) => setTimeout(() => resolve(), 500));
}

function updateBoard(g) {
  var graph = Game.graphToJs(g);
  wobbles.forEach(t => t.reset());
  for (i = 0; i < graph.values.length; i++) {
    if ("c" in graph.values[i]) {
      var c = colorMap[graph.values[i].c].slice();
      if (graph.values[i].v != 0) c.push(0.5/*graph.values[i].v / 10*/);
      circles[i].radius((1 + graph.values[i].v / 10) * r);
      circles[i].fill(color(c));
      circles[i].draw();
    } else {
      circles[i].radius(r);
      circles[i].fill(color([255,255,255,0]));
      circles[i].draw();
    }
    wobbles[i] = new Konva.Tween({
      node: circles[i],
      scaleX: 1.2,
      scaleY: 1.2,
      duration: 0.1,
      onFinish: function() { this.reverse(); }
    });
    var newValue = !vals[i].visible() && graph.values[i].v != 0;
    var increasedValue = graph.values[i].v > parseInt(vals[i].getAttr('text'));
    if (newValue || increasedValue) wobbles[i].play();
    vals[i].setAttr('text', graph.values[i].v);
    vals[i].visible(graph.values[i].v != 0);
    vals[i].draw();
  }
}

function updateScores(ss, he) {
  var scores = Game.scoresToJs(ss);
  var entry = Game.historyEntryToJs(he);

  var cur = scoreBar.getChildren(c => c.getName() == 'gombocka-' + n)[0];
  cur.strokeEnabled(false);
  var nxt = scoreBar.getChildren(c => c.getName() == 'gombocka-' + (n + 1))[0];

  var scr = scoreBar.getChildren(c => c.getName() == 'score-' + n)[0];
  scr.setAttr('text', '' + scores[player]);
  scr.setOffset({ x: scr.getWidth() / 2, y: 0 });

  var lastm = scoreBar.getChildren(c => c.getName() == 'lastm-' + n)[0];
  if ('dir' in entry) lastm.setAttr('text', directionSymbolMap[entry.dir]);
  lastm.setOffset({ x: lastm.getWidth() / 2, y: 0 });

  scoreBar
    .getChildren(c => c.getName() == 'gombocka-' + (n + 1))
    .forEach(c => c.opacity(1));

  appendScoreBar(players + n + 1);

  var shift = new Konva.Tween({
    node: scoreBar,
    easing: Konva.Easings.EaseInOut,
    duration: 0.5,
    x: (n + 1) * -scoreW,
    onFinish: function() {
      nxt.stroke('black');
      nxt.strokeWidth(str / 2);
    }
  });
  shift.play();
}

function gameOver(msg) {
  scoreBar.getChildren(c =>
    parseInt(c.getName().match(/\d+/i)[0]) >= n
  ).forEach(c => c.hide());
  scoreBar.getChildren(c => c.getName() == 'line-' + (n - 1))[0].hide();
  layer.draw();
  var message = new Konva.Text({
    x: cx + (n - 1) * scoreW + 2 * str,
    y: scoreY,
    text: msg,
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  message.setOffset({x: 0, y: message.getHeight() / 2});
  scoreBar.add(message);
  scoreBar.draw();
}

function initBoard(g) {
  var graph = Game.graphToJs(g);

  layer = new Konva.Layer();

  // scores
  scoreBar = new Konva.Group({});
  for (i = 0; i < players + 1; i++) {
    appendScoreBar(i)
  }
  var cur = scoreBar.getChildren(c => c.getName() == 'gombocka-0')[0]
  cur.opacity(1);
  cur.stroke('black');
  cur.strokeWidth(str / 2);
  layer.add(scoreBar);

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
        x: x + str,
        y: y - r / 2 - str,
        text: '0',
        fontFamily: 'Dosis',
        fontStyle: 'bold',
        fontSize: fontSize
      });
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
        tension: 1,
        pointerLength: str,
        pointerWidth: str / 2,
        fill: 'black',
        stroke: 'black',
        strokeWidth: 1,
        opacity: 0.1
      });
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

function appendScoreBar(i) {
  var gombocka = new Konva.Circle({
    name: 'gombocka-' + i,
    x: cx + i * scoreW,
    y: scoreY,
    radius: str,
    fill: color(colorMap[i % players]),
    opacity: 0.1
  });
  scoreBar.add(gombocka);
  var score = new Konva.Text({
    name: 'score-' + i,
    x: cx + i * scoreW,
    y: scoreY - fontSize - 2 * str,
    text: '',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  score.setOffset({x: score.getWidth() / 2, y: 0});
  scoreBar.add(score);
  var lastm = new Konva.Text({
    name: 'lastm-' + i,
    x: cx + i * scoreW,
    y: scoreY + 2 * str,
    text: '',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  lastm.setOffset({x: lastm.getWidth() / 2, y: 0});
  scoreBar.add(lastm);
  var line = new Konva.Line({
    name: 'line-' + i,
    points: [cx + i * scoreW + str, scoreY, cx + i * scoreW + scoreW - str, scoreY],
    fill: 'black',
    stroke: 'black',
    strokeWidth: 1,
    opacity: 0.1
  });
  scoreBar.add(line);
}

function color(arr) {
  if (arr.length > 3) {
    return 'rgba(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ',' + arr[3] + ')';
  } else {
    return 'rgb(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ')';
  }
}
