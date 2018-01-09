const width = window.innerWidth;
const height = window.innerHeight;
const size = Math.min(width, height);       // shorter side of screen
const cx = Math.round(width / 2);           // horizontal center of screen
const r = Math.round(size / 25);            // initial radius of disk
const str = Math.round(r / 3);              // base stroke width factor
const gombs = Math.round(4 * str);          // gombocka spacing
const fontSize = Math.round(r / 2);         // font size
const s = Math.round(3 * r);                // spacing factor btw nodes
const lx = cx - (3 * s);                    // left edge horizontal
const rx = lx + (6 * s);                    // right edge horizontal
const lineY = Math.round(1.5 * r);          // score / control bar vertical
const cy = Math.round(height / 2 + lineY);  // vertical center of screen
const ly = cy - (3 * s);                    // left edge vertical
const ry = ly + (6 * s);                    // right edge vertical


const theOriginal = {
  coordinates: (function() {
    let points = [];
    for (let y = ly; y <= ry; y = y + s + s)
      for (let x = lx; x <= rx; x = x + s + s) points.push({x: x, y: y});
    return points;
  })(),
  directions: [
    {key: 39, symbol: '→'},
    {key: 37, symbol: '←'},
    {key: 40, symbol: '↓'},
    {key: 38, symbol: '↑'},
  ],
  pointers: false
}

const colorMap = [[81,134,198], [229,87,16], [190,255,117]];
const names = ['blue', 'red', 'green']

const stage = new Konva.Stage({
  container: 'container',
  width: width,
  height: height
});
const backgroundLayer = new Konva.Layer();
const graphLayer = new Konva.Layer();
const diskLayer = new Konva.Layer();
const scoreLayer = new Konva.Layer();
const controlLayer = new Konva.Layer();
stage.add(backgroundLayer);
stage.add(graphLayer);
stage.add(diskLayer);
stage.add(scoreLayer);
stage.add(controlLayer);

const line = new Konva.Line({
  points: [0, lineY, width, lineY],
  stroke: 'black',
  strokeWidth: 0.1
});
backgroundLayer.add(line);
backgroundLayer.draw();

let boardConfig;
let board;
let nodes = [];
let arrows = [];
let scrs = [];
let players = 0;
let machines = [];
let msg;

let stored = localStorage.getItem('game');
if (stored) resume(stored)
else configure();

function configure() {
  boardConfig = theOriginal;
  board = Eleven.graphOriginal;
  initBoard();
  addPlayer();
  scoreLayer.draw();
  initConfigControls();
}

function start() {
  initMsg();
  initPlayControls();
  let start = Eleven.start(board, players);
  move(start);
}

function resume(stored) {
  let game = JSON.parse(stored);
  let graph = game.graph;
  boardConfig = theOriginal;
  // TODO This is a hack to replace values with empties in the graph just
  // loaded, so that at restart we start with an empty board, not with
  // the last position loaded from local storage. There must be a better way!
  board = Eleven.graphOf({
    values: graph.values.slice().fill(Eleven.emptyValue),
    edges: graph.edges
  });
  initBoard();
  for (let i = 0; i < game.scores.length; i++) addPlayer();
  initMsg();
  initPlayControls();
  move(Eleven.gameOf(game));
}

function finish(game, farewell) {
  updateScores(game.scores, game.history, game.history.length % players);
  message(farewell);
  localStorage.removeItem('game');
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
      ep = es => new Promise((res, rej) =>
        setTimeout(() => res(Eleven.indexOf(random.put)), 200)
      );
      dp = function(g, ds) {
        postPickEmptyUpdate(g, ds, player);
        return pickDirection(g, ds);
      };
    // machine player moves
    } else if (machines.includes(player)) {
      let best = new Promise(function(resolve, reject) {
        setTimeout(() => resolve(Eleven.historyEntryToJs(Eleven.bestMove(gm))), 200);
      });
      ep = es => best.then(bm =>
        new Promise((res, rej) => res(Eleven.indexOf(bm.put)))
      );
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
    let next = Eleven.move(
      gm,
      Eleven.emptyPickerOf(ep),
      Eleven.directionPickerOf(dp),
      Eleven.resultHandlerOf(rh)
    );
    next.then(move);
  } else if (game.state == 'nomoremoves') {
    finish(game, 'no more valid moves, ' + names[game.winner] + ' wins');
  } else if (game.state == 'eleven') {
    finish(game, names[game.winner] + ' wins with eleven');
  }
}

function pickEmpty(empties) {
  let es = Eleven.emptiesToJs(empties);
  return new Promise(function(resolve, reject) {
    for (let i = 0; i < es.length; i++) {
      nodes[es[i]].disk.on('click', function(evt) {
        nodes.forEach(n => n.disk.off('click'));
        let index = nodes.findIndex(n => n.disk == this);
        resolve(Eleven.indexOf(index));
      });
    }
  });
}

function pickDirection(g, ds) {
  let dirs = Eleven.directionsToJs(ds);
  let keys = dirs.map(d => boardConfig.directions[d].key);
  return new Promise(function(resolve, reject) {
    document.addEventListener('keydown', function _handler(evt) {
      evt.preventDefault();
      if (keys.includes(evt.keyCode)) {
        document.removeEventListener('keydown', _handler, true);
        let direction = boardConfig.directions.findIndex(d => d.key == evt.keyCode);
        resolve(Eleven.directionOf(direction));
      }
    }, true);
  });
}

function postPickEmptyUpdate(g, ds, p) {
  let graph = Eleven.graphToJs(g);
  updateBoard(graph);
  let dirs = Eleven.directionsToJs(ds);
  let syms = dirs.map(d => boardConfig.directions[d].symbol);
  message(names[p] + ' to press ' + syms.join(' or '));
}

function updateBoard(graph) {
  for (let i = 0; i < graph.values.length; i++) {
    if (graph.values[i].c !== undefined) {
      nodes[i].disk.radius((1 + Math.log(graph.values[i].v) / Math.LN2 / 10) * r);
      nodes[i].disk.fill(color(colorMap[graph.values[i].c]));
    } else {
      nodes[i].disk.radius(r);
      nodes[i].disk.fill(color([255,255,255,0]));
    }
    nodes[i].label.setAttr('text', graph.values[i].v);
    nodes[i].label.setOffset({
      x: nodes[i].label.getWidth() / 2,
      y: nodes[i].label.getHeight() / 2
    });
    nodes[i].label.visible(graph.values[i].c !== undefined);
  }
  diskLayer.draw();
}

function updateScores(scores, history, offset) {
  for (let p = 0; p < players; p++) {
    let scr = scrs[p].scr;
    scr.setAttr('text', '' + scores[p]);
    scr.setOffset({x: scr.getWidth() / 2, y: 0});
    if (history[p] && history[p].dir !== undefined) {
      let lastm = scrs[(players + offset - p - 1) % players].lastm;
      lastm.setAttr('text', boardConfig.directions[history[p].dir].symbol);
      lastm.setOffset({x: lastm.getWidth() / 2, y: 0});
    } else {
      scrs[p].lastm.setAttr('text', '');
    }
  }
  scoreLayer.draw();
}

function message(text) {
  msg.setAttr('text', text.toUpperCase());
  scoreLayer.draw();
}

function updateConfigControls(minus, plus, play) {
  play.offsetX(play.getWidth() / 2);
  plus.offsetX(play.getWidth() / 2 + plus.getWidth());
  if (players <= 1) {
    minus.hide();
    plus.show();
  } else if (players >= 3) {
    minus.offsetX(play.getWidth() / 2 + minus.getWidth());
    minus.show();
    plus.hide();
  } else {
    minus.offsetX(play.getWidth() / 2 + plus.getWidth() + minus.getWidth());
    minus.show();
    plus.show();
  }
  controlLayer.draw();
}

function initConfigControls() {
  let xi = rx;
  let minus = new Konva.Text({
    x: xi,
    y: lineY - str,
    width: 2 * str,
    height: 2 * str,
    align: 'center',
    padding: -fontSize / 12 * 7,
    text: '-',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(minus);
  let plus = new Konva.Text({
    x: xi,
    y: lineY - str,
    width: 2 * str,
    height: 2 * str,
    align: 'center',
    padding: -fontSize / 12 * 7,
    text: '+',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(plus);
  let play = new Konva.Text({
    x: xi,
    y: lineY - str,
    width: 2 * str,
    height: 2 * str,
    align: 'center',
    padding: -fontSize / 2,
    text: '▸',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(play);
  plus.on('click', function(evt) {
    addPlayer();
    scoreLayer.draw();
    updateConfigControls(minus, plus, play);
  });
  minus.on('click', function(evt) {
    removePlayer();
    scoreLayer.draw();
    updateConfigControls(minus, plus, play);
  });
  play.on('click', function(evt) {
    minus.hide();
    plus.hide();
    play.hide();
    controlLayer.draw();
    start();
  });
  updateConfigControls(minus, plus, play);
}

function initPlayControls() {
  let xi = rx;
  let restart = new Konva.Text({
    x: xi,
    y: lineY - str,
    width: 2 * str,
    height: 2 * str,
    align: 'center',
    text: '↺',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 1.2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(restart);
  let newgame = new Konva.Text({
    x: xi,
    y: lineY - str,
    width: 2 * str,
    height: 2 * str,
    align: 'center',
    padding: -fontSize / 2,
    text: '▪',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(newgame);
  restart.on('click', function(evt) {
    let start = Eleven.start(board, players);
    localStorage.setItem('game', JSON.stringify(Eleven.gameToJs(start)));
    location.reload(true);
  });
  newgame.on('click', function(evt) {
    localStorage.removeItem('game');
    location.reload(true);
  });
  newgame.offsetX(newgame.getWidth() / 2);
  restart.offsetX(newgame.getWidth() / 2 + restart.getWidth());
  controlLayer.draw();
}

function addPlayer() {
  let i = scrs.length;
  let xi = lx + i * gombs;
  let gombocka = createOrUpdateGombocka(i, xi);
  let score = new Konva.Text({
    name: 'score-' + i,
    x:  xi,
    y: lineY - fontSize - 2 * str,
    text: '',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  score.setOffset({x: score.getWidth() / 2, y: 0});
  scoreLayer.add(score);
  let lastm = new Konva.Text({
    name: 'lastm-' + i,
    x: xi,
    y: lineY + 2 * str,
    text: '',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  lastm.setOffset({x: lastm.getWidth() / 2, y: 0});
  scoreLayer.add(lastm);
  scrs.push({gombocka: gombocka, scr: score, lastm: lastm});
  players++;
}

function removePlayer() {
  let i = scrs.length - 1;
  scrs[i].gombocka.destroy();
  scrs[i].scr.destroy();
  scrs[i].lastm.destroy();
  players--;
  machines = machines.filter(p => p !== i);
  scrs.pop();
}

function createOrUpdateGombocka(i, xi) {
  let gombocka;
  if (machines.includes(i)) {
    gombocka = new Konva.Rect({
      name: 'gombocka-' + i,
      x: xi - str,
      y: lineY - str,
      width: 2 * str,
      height: 2 * str,
      fill: color(colorMap[i])
    })
    gombocka.on('click', function(evt) {
      machines = machines.filter(p => p !== i);
      createOrUpdateGombocka(i, xi);
      scoreLayer.draw();
    });
  } else {
    gombocka = new Konva.Circle({
      name: 'gombocka-' + i,
      x: xi,
      y: lineY,
      radius: str,
      fill: color(colorMap[i])
    });
    gombocka.on('click', function(evt) {
      machines.push(i);
      createOrUpdateGombocka(i, xi);
      scoreLayer.draw();
    });
  }
  if (scrs[i] !== undefined) {
    scrs[i].gombocka.remove();
    scrs[i] = {gombocka: gombocka, scr: scrs[i].scr, lastm: scrs[i].lastm}
  }
  scoreLayer.add(gombocka);
  return gombocka;
}

function initMsg() {
  msg = new Konva.Text({
    x: lx + players * gombs,
    y: lineY - fontSize / 2,
    text: '',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: fontSize,
    fill: 'black'
  });
  scoreLayer.add(msg);
  scoreLayer.draw();
}

function initBoard() {
  let graph = Eleven.graphToJs(board);
  // nodes
  boardConfig.coordinates.forEach(c => {
    let node = new Konva.Circle({
      x: c.x,
      y: c.y,
      radius: str,
      fill: 'black'
    });
    graphLayer.add(node);
    let disk = new Konva.Circle({
      x: c.x,
      y: c.y,
      radius: r,
      fill: color([255,255,255,0])
    });
    diskLayer.add(disk);
    let label = new Konva.Text({
      x: c.x,
      y: c.y,
      text: '',
      fontFamily: 'Dosis',
      fontStyle: 'bold',
      fontSize: 1.5 * fontSize
    });
    label.setOffset({x: label.getWidth() / 2, y: label.getHeight() / 2});
    label.visible(false);
    diskLayer.add(label);
    nodes.push({node: node, disk: disk, label: label});
  });
  // arrows
  for (let d = 0; d < graph.edges.length; d++) {
    let dir = [];
    for (let e = 0; e < graph.edges[d].length; e++) {
      let edge = graph.edges[d][e];
      let x1 = boardConfig.coordinates[edge.from].x;
      let y1 = boardConfig.coordinates[edge.from].y;
      let x2 = boardConfig.coordinates[edge.to].x;
      let y2 = boardConfig.coordinates[edge.to].y;
      let a = new Konva.Arrow({
        points: arrowPoints(x1, y1, x2, y2),
        pointerLength: boardConfig.pointers ? str : 0,
        pointerWidth: boardConfig.pointers ? str : 0,
        fill: 'black',
        stroke: 'black',
        strokeWidth: 0.5
      });
      graphLayer.add(a);
      dir.push(a);
    }
    arrows.push(dir);
  }
  graphLayer.draw();
  diskLayer.draw();
}

function arrowPoints(x1, y1, x2, y2) {
  let a;
  if (x1 === x2 && y1 > y2) a = -Math.PI/2
  else if (x1 === x2 && y2 > y1) a = Math.PI/2
  else if (y1 === y2 && x1 > x2) a = -Math.PI
  else if (y1 === y2 && x2 > x1) a = 0
  else a = Math.atan((y2 - y1) / (x2 - x1));
  let dx = Math.cos(a) * str;
  let dy = Math.sin(a) * str;
  return [x1 + dx, y1 + dy, x2 - dx, y2 - dy];
}

function color(arr) {
  if (arr.length > 3) {
    return 'rgba(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ',' + arr[3] + ')';
  } else {
    return 'rgb(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ')';
  }
}
