const width = window.innerWidth;
const height = window.innerHeight;
const size = Math.min(width, height);     // shorter side of screen
const fontSize = Math.round(size / 50);   // font size
const str = Math.round(size / 75);        // base stroke width factor
const sbsp = Math.round(6 * size / 100);  // score bar spacing factor
const cx = Math.round(width / 2);         // horizontal center of screen
const cy = Math.round(height / 2 + sbsp); // vertical center of screen

const theOriginal = {
  r: Math.round(size / 25),
  get lx () {
    return cx - (9 * this.r);
  },
  get rx () {
    return cx + (9 * this.r);
  },
  get ty () {
    return cy - (9 * this.r);
  },
  get by () {
    return cy + (9 * this.r);
  },
  graph: Eleven.graphToJs(Eleven.theOriginal),
  get coordinates () {
    let points = [];
    for (let y = this.ty; y <= this.by; y = y + 6 * this.r)
      for (let x = this.lx; x <= this.rx; x = x + 6 * this.r)
        points.push({x: x, y: y});
    return points;
  },
  directions: [
    {key: 39, symbol: '→', horizontal: true},
    {key: 37, symbol: '←', horizontal: true},
    {key: 40, symbol: '↓', horizontal: false},
    {key: 38, symbol: '↑', horizontal: false},
  ],
  pointers: false
}

const twoByTwo = {
  r: Math.round(size / 25),
  get lx () {
    return cx - (9 * this.r);
  },
  get rx () {
    return cx + (9 * this.r);
  },
  get ty () {
    return cy - (9 * this.r);
  },
  get by () {
    return cy + (9 * this.r);
  },
  graph: Eleven.graphToJs(Eleven.twoByTwo),
  get coordinates () {
      return [
      {x: this.lx, y: this.ty}, {x: this.rx, y: this.ty},
      {x: this.lx, y: this.by}, {x: this.rx, y: this.by}
    ];
  },
  directions: [
    {key: 39, symbol: '→', horizontal: true},
    {key: 37, symbol: '←', horizontal: true},
    {key: 40, symbol: '↓', horizontal: false},
    {key: 38, symbol: '↑', horizontal: false},
  ],
  pointers: false
}

const theEye = {
  r: Math.round(size / 25),
  get lx () {
    return cx - (18 * this.r);
  },
  get rx () {
    return cx + (18 * this.r);
  },
  get ty () {
    return cy - (9 * this.r);
  },
  get by () {
    return cy + (9 * this.r);
  },
  graph: Eleven.graphToJs(Eleven.theEye),
  get coordinates () {
    let sx = 2 * Math.round(18 * this.r / 6);
    let sy = Math.round(18 * this.r / 7);
    return [
      {x: cx - 3 * sx, y: cy}, // 0
      {x: cx - 2 * sx, y: cy - 2 * sy}, // 1
      {x: cx - 2 * sx, y: cy + 2 * sy}, // 2
      {x: cx - 1 * sx, y: cy - 3 * sy}, // 3
      {x: cx - 1 * sx, y: cy - 1 * sy}, // 4
      {x: cx - 1 * sx, y: cy + 1 * sy}, // 5
      {x: cx - 1 * sx, y: cy + 3 * sy}, // 6
      {x: cx, y: this.ty + 0 * sy}, // 7
      {x: cx, y: this.ty + 1 * sy}, // 8
      {x: cx, y: this.ty + 2 * sy}, // 9
      {x: cx, y: this.ty + 3 * sy}, // 10
      {x: cx, y: this.ty + 4 * sy}, // 11
      {x: cx, y: this.ty + 5 * sy}, // 12
      {x: cx, y: this.ty + 6 * sy}, // 13
      {x: cx, y: this.ty + 7 * sy}, // 14
      {x: cx + 1 * sx, y: cy - 3 * sy}, // 15
      {x: cx + 1 * sx, y: cy - 1 * sy}, // 16
      {x: cx + 1 * sx, y: cy + 1 * sy}, // 17
      {x: cx + 1 * sx, y: cy + 3 * sy}, // 18
      {x: cx + 2 * sx, y: cy - 2 * sy}, // 19
      {x: cx + 2 * sx, y: cy + 2 * sy}, // 20
      {x: cx + 3 * sx, y: cy}, // 21
    ];
  },
  directions: [
    {key: 39, symbol: '→', horizontal: true},
    {key: 37, symbol: '←', horizontal: true},
    {key: 40, symbol: '↓', horizontal: false},
    {key: 38, symbol: '↑', horizontal: false},
  ],
  pointers: false
}

const theCourt = {
  r: Math.round(size / 25),
  get lx () {
    return cx - (9 * this.r);
  },
  get rx () {
    return cx + (9 * this.r);
  },
  get ty () {
    return cy - (9 * this.r);
  },
  get by () {
    return cy + (9 * this.r);
  },
  graph: Eleven.graphToJs(Eleven.theCourt),
  get coordinates () {
    let sy = Math.floor((this.by - this.ty) / 4);
    let slx = Math.floor((this.rx - this.lx) / 2);
    let ssx = Math.floor((this.rx - this.lx) / 3);
    let points = [];
    for (let y = this.ty; y <= this.by; y = y + sy)
      for (let x = this.lx; x <= this.rx; x = x + (((y - this.ty) / sy) % 2 == 1 ? ssx : slx))
        points.push({x: x, y: y});
    return points;
  },
  directions: [
    {key: 39, symbol: '→', horizontal: true},
    {key: 37, symbol: '←', horizontal: true},
    {key: 40, symbol: '↓', horizontal: false},
    {key: 38, symbol: '↑', horizontal: false},
  ],
  pointers: false
}

const boards = [theOriginal, twoByTwo, theCourt];
const colors = [[81,134,198], [229,87,16], [190,255,117]];
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
  points: [0, sbsp, width, sbsp],
  stroke: 'black',
  strokeWidth: 0.1
});
backgroundLayer.add(line);
backgroundLayer.draw();

let board;
let nodes;
let arrows;
let scrs = [];
let players = 0;
let machines;
let msg;

let stored = localStorage.getItem('game');
if (stored !== null) resume(stored)
else configure();

function configure() {
  board = theOriginal;
  machines = [];
  initBoard();
  addPlayer();
  scoreLayer.draw();
  initConfigControls();
}

function start() {
  localStorage.setItem('board', JSON.stringify(board));
  localStorage.setItem('machines', JSON.stringify(machines));
  initBoard();
  initMsg();
  initPlayControls();
  let start = Eleven.start(Eleven.graphOf(board.graph), players);
  move(start);
}

function resume(storedGame) {
  let game = JSON.parse(storedGame);
  let storedBoard = JSON.parse(localStorage.getItem('board'));
  board = storedBoard !== null ? storedBoard : theOriginal;
  initBoard();
  let storedMachines = JSON.parse(localStorage.getItem('machines'));
  machines = storedMachines !== null ? storedMachines : [];
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
  let keys = dirs.map(d => board.directions[d].key);
  return new Promise(function(resolve, reject) {
    document.addEventListener('keydown', function _handler(evt) {
      evt.preventDefault();
      if (keys.includes(evt.keyCode)) {
        document.removeEventListener('keydown', _handler, true);
        let direction = board.directions.findIndex(d => d.key == evt.keyCode);
        resolve(Eleven.directionOf(direction));
      }
    }, true);
  });
}

function postPickEmptyUpdate(g, ds, p) {
  let graph = Eleven.graphToJs(g);
  updateBoard(graph);
  let dirs = Eleven.directionsToJs(ds);
  let syms = dirs.map(d => board.directions[d].symbol);
  message(names[p] + ' to press ' + syms.join(' or '));
}

function updateBoard(graph) {
  for (let i = 0; i < graph.values.length; i++) {
    if (graph.values[i].c !== undefined) {
      nodes[i].disk.radius((1 + Math.log(graph.values[i].v) / Math.LN2 / 10) * board.r);
      nodes[i].disk.fill(color(colors[graph.values[i].c]));
    } else {
      nodes[i].disk.radius(board.r);
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
      lastm.setAttr('text', board.directions[history[p].dir].symbol);
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
  let xi = width - board.r;
  let minus = new Konva.Text({
    x: xi,
    y: sbsp - str,
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
    y: sbsp - str,
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
    y: sbsp - str,
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
  let previousB = new Konva.Text({
    x: board.r,
    y: cy,
    width: 2 * fontSize,
    height: 2 * fontSize,
    align: 'center',
    text: '↩',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(previousB);
  previousB.offsetX(previousB.getWidth() / 2);
  let nextB = new Konva.Text({
    x: width - board.r,
    y: cy,
    width: 2 * fontSize,
    height: 2 * fontSize,
    align: 'center',
    text: '↪',
    fontFamily: 'Dosis',
    fontStyle: 'bold',
    fontSize: 2 * fontSize,
    fill: 'black'
  });
  controlLayer.add(nextB);
  nextB.offsetX(nextB.getWidth() / 2);
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
    previousB.hide();
    nextB.hide();
    controlLayer.draw();
    start();
  });
  previousB.on('click', function(evt) {
    let i = (boards.indexOf(board) + boards.length - 1) % boards.length;
    board = boards[i];
    graphLayer.removeChildren();
    diskLayer.removeChildren();
    initBoard();
  });
  nextB.on('click', function(evt) {
    let i = (boards.indexOf(board) + 1) % boards.length;
    board = boards[i];
    graphLayer.removeChildren();
    diskLayer.removeChildren();
    initBoard();
  });
  updateConfigControls(minus, plus, play);
}

function initPlayControls() {
  let xi = width - board.r;
  let restart = new Konva.Text({
    x: xi,
    y: sbsp - str,
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
    y: sbsp - str,
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
    let start = Eleven.start(Eleven.graphOf(board.graph), players);
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
  let xi = board.r + i * sbsp;
  let gombocka = createOrUpdateGombocka(i, xi);
  let score = new Konva.Text({
    name: 'score-' + i,
    x:  xi,
    y: sbsp - fontSize - 2 * str,
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
    y: sbsp + 2 * str,
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
      y: sbsp - str,
      width: 2 * str,
      height: 2 * str,
      fill: color(colors[i])
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
      y: sbsp,
      radius: str,
      fill: color(colors[i])
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
    x: board.r + players * sbsp,
    y: sbsp - fontSize / 2,
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
  nodes = [];
  arrows = [];
  // nodes
  board.coordinates.forEach(c => {
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
      radius: board.r,
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
  for (let d = 0; d < board.graph.dirs.length; d++) {
    let dir = [];
    for (let e = 0; e < board.graph.dirs[d].edges.length; e++) {
      let edge = board.graph.dirs[d].edges[e];
      let x1 = board.coordinates[edge.from].x;
      let y1 = board.coordinates[edge.from].y;
      let x2 = board.coordinates[edge.to].x;
      let y2 = board.coordinates[edge.to].y;
      let points;
      if (board.directions[d].horizontal) {
        let offset = x2 > x1 ? str : -str;
        let middle = (x2 - x1) / 2 + x1;
        points = [x1 + offset, y1, middle, y1, middle, y2, x2 - offset, y2];
      } else {
        let offset = y2 > y1 ? str : -str;
        let middle = (y2 - y1) / 2 + y1;
        points = [x1, y1 + offset, x1, middle, x2, middle, x2, y2 - offset];
      }
      let a = new Konva.Arrow({
        points: points,
        pointerLength: board.pointers ? str : 0,
        pointerWidth: board.pointers ? str : 0,
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

function color(arr) {
  if (arr.length > 3) {
    return 'rgba(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ',' + arr[3] + ')';
  } else {
    return 'rgb(' + arr[0] + ',' + arr[1] + ',' + arr[2] + ')';
  }
}
