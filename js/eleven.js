var orig = Game.graphToJs(Game.graphOriginal);
console.log(orig);

var width = window.innerWidth;
var height = window.innerHeight;
var size = Math.min(width, height);

var cx = Math.round(width / 2);
var cy = Math.round(height / 2);
var r = Math.round(size / 15);
var str = Math.round(r / 10);
var s = Math.round(r * 3 / 2);

var circles = new Array();

var layer = new Konva.Layer();

for (y = cy - (3 * s); y <= cy + (3 * s); y = y + s + s) {
  for (x = cx - (3 * s); x <= cx + (3 * s); x = x + s + s) {
    c = new Konva.Circle({
      x: x,
      y: y,
      radius: r,
      stroke: 'black',
      strokeWidth: str
    });
    circles.push(c);
    layer.add(c);
  }
}

var arrows = new Array();

for (d = 0; d < orig.edges.length; d++) {
  arrows.push([]);
  for (e = 0; e < orig.edges[d].length; e++) {
    var edge = orig.edges[d][e];
    var from = circles[edge.from];
    var to = circles[edge.to];
    var x1 = from.attrs.x;
    var y1 = from.attrs.y;
    var x2 = to.attrs.x;
    var y2 = to.attrs.y;
    var a = new Konva.Arrow({
      points: arrowPoints(x1, y1, x2, y2),
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

pickEmpty().then(function(i) { console.log(i);  }, function(err) {console.log(err); });

var stage = new Konva.Stage({
  container: 'container',
  width: width,
  height: height
});

stage.add(layer);

function arrowPoints(x1, y1, x2, y2) {
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

function pickEmpty() {
  return new Promise(function(resolve, reject) {
    for (i = 0; i < circles.length; i++) {
      circles[i].on("click", function(evt) {
        this.fill('red');
        this.draw();
        circles.forEach(c => c.off("click"));
        var index = circles.findIndex(c => c == this);
        resolve(index);
      });
    }
  });
}
