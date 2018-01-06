const players = 3;
const machines = [0,2];
const colorMap = [[81,134,198], [229,87,16], [190,255,117]];
const names = ['blue', 'red', 'green']

const graph = Eleven.graphOriginal;
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

let circles = [];
let vals = [];
let scrs = [];

let layer;
let msg;

initBoard(graph);

let stored = localStorage.getItem('game');
if (stored) {
  let con = Eleven.gameOf(JSON.parse(stored));
  move(con);
} else {
  let start = Eleven.start(graph, players);
  move(start);
}

function move(gm) {
  let game = Eleven.gameToJs(gm);
  localStorage.setItem('game', JSON.stringify(game));
  let player = game.history.length % players;
  updateBoard(game.graph);
  updateScores(game.scores, game.history, player);
  message(names[player] + ' to pick an empty node');
  if (game.state == 'continued') {
    let ep, dp;
    let rh = (he, g, ss) => new Promise((resolve, reject) => resolve());
    // single player moves
    if (players == 1) {
      let rm = Eleven.randomEmpty(gm);
      let random = Eleven.historyEntryToJs(rm);
      ep = es => new Promise((res, rej) => res(Eleven.indexOf(random.put)));
      dp = function(g, ds) {
        postPickEmptyUpdate(g, ds, player);
        return pickDirection(g, ds);
      };
    // machine player moves
    } else if (machines.includes(player)) {
      let best = new Promise(function(resolve, reject) {
        setTimeout(() => resolve(Eleven.historyEntryToJs(Eleven.bestMove(gm))), 200);
      });
      ep = es => best.then(bm => new Promise((res, rej) => res(Eleven.indexOf(bm.put))));
      dp = (g, ds) => best.then(bm => new Promise(function(resolve, reject) {
        postPickEmptyUpdate(g, ds, player)
        setTimeout(() => resolve(Eleven.directionOf(bm.dir)), 1000);
      }));
    // human player moves
    } else {
      ep = pickEmpty;
      dp = function(g, ds) {
        postPickEmptyUpdate(g, ds, player);
        return pickDirection(g, ds);
      }
    }
    let next = Eleven.move(gm, Eleven.emptyPickerOf(ep), Eleven.directionPickerOf(dp), Eleven.resultHandlerOf(rh));
    next.then(move);
  } else if (game.state == 'nomoremoves') {
    message('no more valid moves, ' + names[game.winner] + ' wins with ' + game.scores[game.winner] + ' points');
    localStorage.removeItem('game');
  } else if (game.state == 'eleven') {
    message(names[game.winner] + ' wins with eleven');
    localStorage.removeItem('game');
  }
}

function pickEmpty(empties) {
  let es = Eleven.emptiesToJs(empties);
  return new Promise(function(resolve, reject) {
    for (let i = 0; i < es.length; i++) {
      circles[es[i]].on("click", function(evt) {
        circles.forEach(c => c.off("click"));
        let index = circles.findIndex(c => c == this);
        resolve(Eleven.indexOf(index));
      });
    }
  });
}

function pickDirection(graph, directions) {
  let dirs = Eleven.directionsToJs(directions);
  let keys = dirs.map(d => directionKeyMap[d]);
  return new Promise(function(resolve, reject) {
    document.addEventListener('keydown', function _handler(evt) {
      evt.preventDefault();
      if (keys.includes(evt.keyCode)) {
        document.removeEventListener('keydown', _handler, false);
        let direction = directionKeyMap.indexOf(evt.keyCode);
        resolve(Eleven.directionOf(direction));
      }
    }, false);
  });
}

function postPickEmptyUpdate(g, ds, p) {
  let graph = Eleven.graphToJs(g);
  let dirs = Eleven.directionsToJs(ds);
  updateBoard(graph);
  let syms = dirs.map(d => directionSymbolMap[d]);
  message(names[p] + ' to press ' + syms.join(' or '));
}

function updateBoard(graph) {
  for (let i = 0; i < graph.values.length; i++) {
    if (graph.values[i].c !== undefined) {
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

function updateScores(scores, history, player) {
  for (let p = 0; p < players; p++) {
    let scr = scrs[p].scr;
    scr.setAttr('text', '' + scores[p]);
    scr.setOffset({ x: scr.getWidth() / 2, y: 0 });
    if (history[p] && history[p].dir !== undefined) {
      let lastm = scrs[(players + player - p - 1) % players].lastm;
      lastm.setAttr('text', directionSymbolMap[history[p].dir]);
      lastm.setOffset({ x: lastm.getWidth() / 2, y: 0 });
    }
  }
  layer.draw();
}

function message(text) {
  msg.setAttr('text', text.toUpperCase());
  layer.draw();
}

function initBoard(g) {
  let graph = Eleven.graphToJs(g);

  layer = new Konva.Layer();

  // scores
  let line = new Konva.Line({
    points: [0, scoreY, width, scoreY],
    stroke: 'black',
    strokeWidth: 0.1
  });
  layer.add(line);
  for (let i = 0; i < players; i++) {
    let xi = r + i * 2 * r;
    let gombocka = new Konva.Circle({
      name: 'gombocka-' + i,
      x: xi,
      y: scoreY,
      radius: str,
      fill: color(colorMap[i])
    });
    layer.add(gombocka);
    let score = new Konva.Text({
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
    let lastm = new Konva.Text({
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
  msg = new Konva.Text({
    x: r + players * 2 * r,
    y: scoreY - fontSize / 2,
    text: '',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  layer.add(msg);

  // circles
  for (let y = cy - (3 * s); y <= cy + (3 * s); y = y + s + s) {
    for (let x = cx - (3 * s); x <= cx + (3 * s); x = x + s + s) {
      let n = new Konva.Circle({
        x: x,
        y: y,
        radius: str,
        fill: 'black'
      });
      layer.add(n);
      let c = new Konva.Circle({
        x: x,
        y: y,
        radius: r,
        fill: color([255,255,255,0])
      });
      circles.push(c);
      layer.add(c);
      let t = new Konva.Text({
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
  for (let d = 0; d < graph.edges.length; d++) {
    for (let e = 0; e < graph.edges[d].length; e++) {
      let edge = graph.edges[d][e];
      let from = circles[edge.from];
      let to = circles[edge.to];
      let x1 = from.attrs.x;
      let y1 = from.attrs.y;
      let x2 = to.attrs.x;
      let y2 = to.attrs.y;
      let a = new Konva.Arrow({
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

  let stage = new Konva.Stage({
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
