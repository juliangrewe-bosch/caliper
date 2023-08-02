/*
 Highcharts JS v5.0.3 (2016-11-18)

 (c) 2009-2016 Torstein Honsi

 License: www.highcharts.com/license
*/
(function (x) {
  "object" === typeof module && module.exports
    ? (module.exports = x)
    : x(Highcharts);
})(function (x) {
  (function (b) {
    function r(b, a, d) {
      this.init(b, a, d);
    }
    var t = b.each,
      w = b.extend,
      m = b.merge,
      q = b.splat;
    w(r.prototype, {
      init: function (b, a, d) {
        var f = this,
          h = f.defaultOptions;
        f.chart = a;
        f.options = b = m(h, a.angular ? { background: {} } : void 0, b);
        (b = b.background) &&
          t([].concat(q(b)).reverse(), function (a) {
            var c,
              h = d.userOptions;
            c = m(f.defaultBackgroundOptions, a);
            a.backgroundColor && (c.backgroundColor = a.backgroundColor);
            c.color = c.backgroundColor;
            d.options.plotBands.unshift(c);
            h.plotBands = h.plotBands || [];
            h.plotBands !== d.options.plotBands && h.plotBands.unshift(c);
          });
      },
      defaultOptions: { center: ["50%", "50%"], size: "85%", startAngle: 0 },
      defaultBackgroundOptions: {
        className: "highcharts-pane",
        shape: "circle",
        borderWidth: 1,
        borderColor: "#cccccc",
        backgroundColor: {
          linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
          stops: [
            [0, "#ffffff"],
            [1, "#e6e6e6"],
          ],
        },
        from: -Number.MAX_VALUE,
        innerRadius: 0,
        to: Number.MAX_VALUE,
        outerRadius: "105%",
      },
    });
    b.Pane = r;
  })(x);
  (function (b) {
    var r = b.CenteredSeriesMixin,
      t = b.each,
      w = b.extend,
      m = b.map,
      q = b.merge,
      e = b.noop,
      a = b.Pane,
      d = b.pick,
      f = b.pInt,
      h = b.splat,
      u = b.wrap,
      c,
      l,
      k = b.Axis.prototype;
    b = b.Tick.prototype;
    c = {
      getOffset: e,
      redraw: function () {
        this.isDirty = !1;
      },
      render: function () {
        this.isDirty = !1;
      },
      setScale: e,
      setCategories: e,
      setTitle: e,
    };
    l = {
      defaultRadialGaugeOptions: {
        labels: { align: "center", x: 0, y: null },
        minorGridLineWidth: 0,
        minorTickInterval: "auto",
        minorTickLength: 10,
        minorTickPosition: "inside",
        minorTickWidth: 1,
        tickLength: 10,
        tickPosition: "inside",
        tickWidth: 2,
        title: { rotation: 0 },
        zIndex: 2,
      },
      defaultRadialXOptions: {
        gridLineWidth: 1,
        labels: { align: null, distance: 15, x: 0, y: null },
        maxPadding: 0,
        minPadding: 0,
        showLastLabel: !1,
        tickLength: 0,
      },
      defaultRadialYOptions: {
        gridLineInterpolation: "circle",
        labels: { align: "right", x: -3, y: -2 },
        showLastLabel: !1,
        title: { x: 4, text: null, rotation: 90 },
      },
      setOptions: function (a) {
        a = this.options = q(this.defaultOptions, this.defaultRadialOptions, a);
        a.plotBands || (a.plotBands = []);
      },
      getOffset: function () {
        k.getOffset.call(this);
        this.chart.axisOffset[this.side] = 0;
        this.center = this.pane.center = r.getCenter.call(this.pane);
      },
      getLinePath: function (a, g) {
        a = this.center;
        var c = this.chart,
          f = d(g, a[2] / 2 - this.offset);
        this.isCircular || void 0 !== g
          ? (g = this.chart.renderer.symbols.arc(
              this.left + a[0],
              this.top + a[1],
              f,
              f,
              {
                start: this.startAngleRad,
                end: this.endAngleRad,
                open: !0,
                innerR: 0,
              }
            ))
          : ((g = this.postTranslate(this.angleRad, f)),
            (g = ["M", a[0] + c.plotLeft, a[1] + c.plotTop, "L", g.x, g.y]));
        return g;
      },
      setAxisTranslation: function () {
        k.setAxisTranslation.call(this);
        this.center &&
          ((this.transA = this.isCircular
            ? (this.endAngleRad - this.startAngleRad) /
              (this.max - this.min || 1)
            : this.center[2] / 2 / (this.max - this.min || 1)),
          (this.minPixelPadding = this.isXAxis
            ? this.transA * this.minPointOffset
            : 0));
      },
      beforeSetTickPositions: function () {
        if (
          (this.autoConnect =
            this.isCircular &&
            void 0 === d(this.userMax, this.options.max) &&
            this.endAngleRad - this.startAngleRad === 2 * Math.PI)
        )
          this.max +=
            (this.categories && 1) ||
            this.pointRange ||
            this.closestPointRange ||
            0;
      },
      setAxisSize: function () {
        k.setAxisSize.call(this);
        this.isRadial &&
          ((this.center = this.pane.center = r.getCenter.call(this.pane)),
          this.isCircular &&
            (this.sector = this.endAngleRad - this.startAngleRad),
          (this.len =
            this.width =
            this.height =
              (this.center[2] * d(this.sector, 1)) / 2));
      },
      getPosition: function (a, g) {
        return this.postTranslate(
          this.isCircular ? this.translate(a) : this.angleRad,
          d(this.isCircular ? g : this.translate(a), this.center[2] / 2) -
            this.offset
        );
      },
      postTranslate: function (a, g) {
        var d = this.chart,
          c = this.center;
        a = this.startAngleRad + a;
        return {
          x: d.plotLeft + c[0] + Math.cos(a) * g,
          y: d.plotTop + c[1] + Math.sin(a) * g,
        };
      },
      getPlotBandPath: function (a, g, c) {
        var h = this.center,
          p = this.startAngleRad,
          k = h[2] / 2,
          n = [d(c.outerRadius, "100%"), c.innerRadius, d(c.thickness, 10)],
          b = Math.min(this.offset, 0),
          l = /%$/,
          u,
          e = this.isCircular;
        "polygon" === this.options.gridLineInterpolation
          ? (h = this.getPlotLinePath(a).concat(this.getPlotLinePath(g, !0)))
          : ((a = Math.max(a, this.min)),
            (g = Math.min(g, this.max)),
            e || ((n[0] = this.translate(a)), (n[1] = this.translate(g))),
            (n = m(n, function (a) {
              l.test(a) && (a = (f(a, 10) * k) / 100);
              return a;
            })),
            "circle" !== c.shape && e
              ? ((a = p + this.translate(a)), (g = p + this.translate(g)))
              : ((a = -Math.PI / 2), (g = 1.5 * Math.PI), (u = !0)),
            (n[0] -= b),
            (n[2] -= b),
            (h = this.chart.renderer.symbols.arc(
              this.left + h[0],
              this.top + h[1],
              n[0],
              n[0],
              {
                start: Math.min(a, g),
                end: Math.max(a, g),
                innerR: d(n[1], n[0] - n[2]),
                open: u,
              }
            )));
        return h;
      },
      getPlotLinePath: function (a, g) {
        var d = this,
          c = d.center,
          f = d.chart,
          h = d.getPosition(a),
          k,
          b,
          p;
        d.isCircular
          ? (p = ["M", c[0] + f.plotLeft, c[1] + f.plotTop, "L", h.x, h.y])
          : "circle" === d.options.gridLineInterpolation
          ? (a = d.translate(a)) && (p = d.getLinePath(0, a))
          : (t(f.xAxis, function (a) {
              a.pane === d.pane && (k = a);
            }),
            (p = []),
            (a = d.translate(a)),
            (c = k.tickPositions),
            k.autoConnect && (c = c.concat([c[0]])),
            g && (c = [].concat(c).reverse()),
            t(c, function (g, d) {
              b = k.getPosition(g, a);
              p.push(d ? "L" : "M", b.x, b.y);
            }));
        return p;
      },
      getTitlePosition: function () {
        var a = this.center,
          g = this.chart,
          d = this.options.title;
        return {
          x: g.plotLeft + a[0] + (d.x || 0),
          y:
            g.plotTop +
            a[1] -
            { high: 0.5, middle: 0.25, low: 0 }[d.align] * a[2] +
            (d.y || 0),
        };
      },
    };
    u(k, "init", function (f, g, k) {
      var b = g.angular,
        p = g.polar,
        n = k.isX,
        u = b && n,
        e,
        A = g.options,
        m = k.pane || 0;
      if (b) {
        if ((w(this, u ? c : l), (e = !n)))
          this.defaultRadialOptions = this.defaultRadialGaugeOptions;
      } else p && (w(this, l), (this.defaultRadialOptions = (e = n) ? this.defaultRadialXOptions : q(this.defaultYAxisOptions, this.defaultRadialYOptions)));
      b || p
        ? ((this.isRadial = !0), (g.inverted = !1), (A.chart.zoomType = null))
        : (this.isRadial = !1);
      f.call(this, g, k);
      u ||
        (!b && !p) ||
        ((f = this.options),
        g.panes || (g.panes = []),
        (this.pane =
          g =
          g.panes[m] =
            g.panes[m] || new a(h(A.pane)[m], g, this)),
        (g = g.options),
        (this.angleRad = ((f.angle || 0) * Math.PI) / 180),
        (this.startAngleRad = ((g.startAngle - 90) * Math.PI) / 180),
        (this.endAngleRad =
          ((d(g.endAngle, g.startAngle + 360) - 90) * Math.PI) / 180),
        (this.offset = f.offset || 0),
        (this.isCircular = e));
    });
    u(k, "autoLabelAlign", function (a) {
      if (!this.isRadial) return a.apply(this, [].slice.call(arguments, 1));
    });
    u(b, "getPosition", function (a, d, c, f, h) {
      var g = this.axis;
      return g.getPosition ? g.getPosition(c) : a.call(this, d, c, f, h);
    });
    u(b, "getLabelPosition", function (a, g, c, f, h, k, b, l, u) {
      var n = this.axis,
        p = k.y,
        e = 20,
        y = k.align,
        v =
          (((n.translate(this.pos) + n.startAngleRad + Math.PI / 2) / Math.PI) *
            180) %
          360;
      n.isRadial
        ? ((a = n.getPosition(this.pos, n.center[2] / 2 + d(k.distance, -25))),
          "auto" === k.rotation
            ? f.attr({ rotation: v })
            : null === p &&
              (p =
                n.chart.renderer.fontMetrics(f.styles.fontSize).b -
                f.getBBox().height / 2),
          null === y &&
            (n.isCircular
              ? (this.label.getBBox().width >
                  (n.len * n.tickInterval) / (n.max - n.min) && (e = 0),
                (y =
                  v > e && v < 180 - e
                    ? "left"
                    : v > 180 + e && v < 360 - e
                    ? "right"
                    : "center"))
              : (y = "center"),
            f.attr({ align: y })),
          (a.x += k.x),
          (a.y += p))
        : (a = a.call(this, g, c, f, h, k, b, l, u));
      return a;
    });
    u(b, "getMarkPath", function (a, d, c, f, h, k, b) {
      var g = this.axis;
      g.isRadial
        ? ((a = g.getPosition(this.pos, g.center[2] / 2 + f)),
          (d = ["M", d, c, "L", a.x, a.y]))
        : (d = a.call(this, d, c, f, h, k, b));
      return d;
    });
  })(x);
  (function (b) {
    var r = b.each,
      t = b.noop,
      w = b.pick,
      m = b.Series,
      q = b.seriesType,
      e = b.seriesTypes;
    q(
      "arearange",
      "area",
      {
        lineWidth: 1,
        marker: null,
        threshold: null,
        tooltip: {
          pointFormat:
            '\x3cspan style\x3d"color:{series.color}"\x3e\u25cf\x3c/span\x3e {series.name}: \x3cb\x3e{point.low}\x3c/b\x3e - \x3cb\x3e{point.high}\x3c/b\x3e\x3cbr/\x3e',
        },
        trackByArea: !0,
        dataLabels: {
          align: null,
          verticalAlign: null,
          xLow: 0,
          xHigh: 0,
          yLow: 0,
          yHigh: 0,
        },
        states: { hover: { halo: !1 } },
      },
      {
        pointArrayMap: ["low", "high"],
        dataLabelCollections: ["dataLabel", "dataLabelUpper"],
        toYData: function (a) {
          return [a.low, a.high];
        },
        pointValKey: "low",
        deferTranslatePolar: !0,
        highToXY: function (a) {
          var d = this.chart,
            f = this.xAxis.postTranslate(
              a.rectPlotX,
              this.yAxis.len - a.plotHigh
            );
          a.plotHighX = f.x - d.plotLeft;
          a.plotHigh = f.y - d.plotTop;
        },
        translate: function () {
          var a = this,
            d = a.yAxis,
            f = !!a.modifyValue;
          e.area.prototype.translate.apply(a);
          r(a.points, function (h) {
            var b = h.low,
              c = h.high,
              l = h.plotY;
            null === c || null === b
              ? (h.isNull = !0)
              : ((h.plotLow = l),
                (h.plotHigh = d.translate(
                  f ? a.modifyValue(c, h) : c,
                  0,
                  1,
                  0,
                  1
                )),
                f && (h.yBottom = h.plotHigh));
          });
          this.chart.polar &&
            r(this.points, function (d) {
              a.highToXY(d);
            });
        },
        getGraphPath: function (a) {
          var d = [],
            f = [],
            h,
            b = e.area.prototype.getGraphPath,
            c,
            l,
            k;
          k = this.options;
          var p = k.step;
          a = a || this.points;
          for (h = a.length; h--; )
            (c = a[h]),
              c.isNull ||
                k.connectEnds ||
                (a[h + 1] && !a[h + 1].isNull) ||
                f.push({ plotX: c.plotX, plotY: c.plotY, doCurve: !1 }),
              (l = {
                polarPlotY: c.polarPlotY,
                rectPlotX: c.rectPlotX,
                yBottom: c.yBottom,
                plotX: w(c.plotHighX, c.plotX),
                plotY: c.plotHigh,
                isNull: c.isNull,
              }),
              f.push(l),
              d.push(l),
              c.isNull ||
                k.connectEnds ||
                (a[h - 1] && !a[h - 1].isNull) ||
                f.push({ plotX: c.plotX, plotY: c.plotY, doCurve: !1 });
          a = b.call(this, a);
          p &&
            (!0 === p && (p = "left"),
            (k.step = { left: "right", center: "center", right: "left" }[p]));
          d = b.call(this, d);
          f = b.call(this, f);
          k.step = p;
          k = [].concat(a, d);
          this.chart.polar || "M" !== f[0] || (f[0] = "L");
          this.graphPath = k;
          this.areaPath = this.areaPath.concat(a, f);
          k.isArea = !0;
          k.xMap = a.xMap;
          this.areaPath.xMap = a.xMap;
          return k;
        },
        drawDataLabels: function () {
          var a = this.data,
            d = a.length,
            f,
            h = [],
            b = m.prototype,
            c = this.options.dataLabels,
            l = c.align,
            k = c.verticalAlign,
            p = c.inside,
            g,
            n,
            e = this.chart.inverted;
          if (c.enabled || this._hasPointLabels) {
            for (f = d; f--; )
              if ((g = a[f]))
                (n = p ? g.plotHigh < g.plotLow : g.plotHigh > g.plotLow),
                  (g.y = g.high),
                  (g._plotY = g.plotY),
                  (g.plotY = g.plotHigh),
                  (h[f] = g.dataLabel),
                  (g.dataLabel = g.dataLabelUpper),
                  (g.below = n),
                  e
                    ? l || (c.align = n ? "right" : "left")
                    : k || (c.verticalAlign = n ? "top" : "bottom"),
                  (c.x = c.xHigh),
                  (c.y = c.yHigh);
            b.drawDataLabels && b.drawDataLabels.apply(this, arguments);
            for (f = d; f--; )
              if ((g = a[f]))
                (n = p ? g.plotHigh < g.plotLow : g.plotHigh > g.plotLow),
                  (g.dataLabelUpper = g.dataLabel),
                  (g.dataLabel = h[f]),
                  (g.y = g.low),
                  (g.plotY = g._plotY),
                  (g.below = !n),
                  e
                    ? l || (c.align = n ? "left" : "right")
                    : k || (c.verticalAlign = n ? "bottom" : "top"),
                  (c.x = c.xLow),
                  (c.y = c.yLow);
            b.drawDataLabels && b.drawDataLabels.apply(this, arguments);
          }
          c.align = l;
          c.verticalAlign = k;
        },
        alignDataLabel: function () {
          e.column.prototype.alignDataLabel.apply(this, arguments);
        },
        setStackedPoints: t,
        getSymbol: t,
        drawPoints: t,
      }
    );
  })(x);
  (function (b) {
    var r = b.seriesType;
    r("areasplinerange", "arearange", null, {
      getPointSpline: b.seriesTypes.spline.prototype.getPointSpline,
    });
  })(x);
  (function (b) {
    var r = b.defaultPlotOptions,
      t = b.each,
      w = b.merge,
      m = b.noop,
      q = b.pick,
      e = b.seriesType,
      a = b.seriesTypes.column.prototype;
    e(
      "columnrange",
      "arearange",
      w(r.column, r.arearange, { lineWidth: 1, pointRange: null }),
      {
        translate: function () {
          var d = this,
            f = d.yAxis,
            b = d.xAxis,
            u = b.startAngleRad,
            c,
            l = d.chart,
            k = d.xAxis.isRadial,
            p;
          a.translate.apply(d);
          t(d.points, function (a) {
            var g = a.shapeArgs,
              h = d.options.minPointLength,
              e,
              v;
            a.plotHigh = p = f.translate(a.high, 0, 1, 0, 1);
            a.plotLow = a.plotY;
            v = p;
            e = q(a.rectPlotY, a.plotY) - p;
            Math.abs(e) < h
              ? ((h -= e), (e += h), (v -= h / 2))
              : 0 > e && ((e *= -1), (v -= e));
            k
              ? ((c = a.barX + u),
                (a.shapeType = "path"),
                (a.shapeArgs = {
                  d: d.polarArc(v + e, v, c, c + a.pointWidth),
                }))
              : ((g.height = e),
                (g.y = v),
                (a.tooltipPos = l.inverted
                  ? [
                      f.len + f.pos - l.plotLeft - v - e / 2,
                      b.len + b.pos - l.plotTop - g.x - g.width / 2,
                      e,
                    ]
                  : [
                      b.left - l.plotLeft + g.x + g.width / 2,
                      f.pos - l.plotTop + v + e / 2,
                      e,
                    ]));
          });
        },
        directTouch: !0,
        trackerGroups: ["group", "dataLabelsGroup"],
        drawGraph: m,
        crispCol: a.crispCol,
        drawPoints: a.drawPoints,
        drawTracker: a.drawTracker,
        getColumnMetrics: a.getColumnMetrics,
        animate: function () {
          return a.animate.apply(this, arguments);
        },
        polarArc: function () {
          return a.polarArc.apply(this, arguments);
        },
        pointAttribs: a.pointAttribs,
      }
    );
  })(x);
  (function (b) {
    var r = b.each,
      t = b.isNumber,
      w = b.merge,
      m = b.pick,
      q = b.pInt,
      e = b.Series,
      a = b.seriesType,
      d = b.TrackerMixin;
    a(
      "gauge",
      "line",
      {
        dataLabels: {
          enabled: !0,
          defer: !1,
          y: 15,
          borderRadius: 3,
          crop: !1,
          verticalAlign: "top",
          zIndex: 2,
          borderWidth: 1,
          borderColor: "#cccccc",
        },
        dial: {},
        pivot: {},
        tooltip: { headerFormat: "" },
        showInLegend: !1,
      },
      {
        angular: !0,
        directTouch: !0,
        drawGraph: b.noop,
        fixedBox: !0,
        forceDL: !0,
        noSharedTooltip: !0,
        trackerGroups: ["group", "dataLabelsGroup"],
        translate: function () {
          var a = this.yAxis,
            d = this.options,
            b = a.center;
          this.generatePoints();
          r(this.points, function (c) {
            var f = w(d.dial, c.dial),
              k = (q(m(f.radius, 80)) * b[2]) / 200,
              h = (q(m(f.baseLength, 70)) * k) / 100,
              g = (q(m(f.rearLength, 10)) * k) / 100,
              n = f.baseWidth || 3,
              u = f.topWidth || 1,
              e = d.overshoot,
              v = a.startAngleRad + a.translate(c.y, null, null, null, !0);
            t(e)
              ? ((e = (e / 180) * Math.PI),
                (v = Math.max(
                  a.startAngleRad - e,
                  Math.min(a.endAngleRad + e, v)
                )))
              : !1 === d.wrap &&
                (v = Math.max(a.startAngleRad, Math.min(a.endAngleRad, v)));
            v = (180 * v) / Math.PI;
            c.shapeType = "path";
            c.shapeArgs = {
              d: f.path || [
                "M",
                -g,
                -n / 2,
                "L",
                h,
                -n / 2,
                k,
                -u / 2,
                k,
                u / 2,
                h,
                n / 2,
                -g,
                n / 2,
                "z",
              ],
              translateX: b[0],
              translateY: b[1],
              rotation: v,
            };
            c.plotX = b[0];
            c.plotY = b[1];
          });
        },
        drawPoints: function () {
          var a = this,
            d = a.yAxis.center,
            b = a.pivot,
            c = a.options,
            l = c.pivot,
            k = a.chart.renderer;
          r(a.points, function (d) {
            var g = d.graphic,
              b = d.shapeArgs,
              f = b.d,
              h = w(c.dial, d.dial);
            g
              ? (g.animate(b), (b.d = f))
              : ((d.graphic = k[d.shapeType](b)
                  .attr({ rotation: b.rotation, zIndex: 1 })
                  .addClass("highcharts-dial")
                  .add(a.group)),
                d.graphic.attr({
                  stroke: h.borderColor || "none",
                  "stroke-width": h.borderWidth || 0,
                  fill: h.backgroundColor || "#000000",
                }));
          });
          b
            ? b.animate({ translateX: d[0], translateY: d[1] })
            : ((a.pivot = k
                .circle(0, 0, m(l.radius, 5))
                .attr({ zIndex: 2 })
                .addClass("highcharts-pivot")
                .translate(d[0], d[1])
                .add(a.group)),
              a.pivot.attr({
                "stroke-width": l.borderWidth || 0,
                stroke: l.borderColor || "#cccccc",
                fill: l.backgroundColor || "#000000",
              }));
        },
        animate: function (a) {
          var d = this;
          a ||
            (r(d.points, function (a) {
              var c = a.graphic;
              c &&
                (c.attr({ rotation: (180 * d.yAxis.startAngleRad) / Math.PI }),
                c.animate(
                  { rotation: a.shapeArgs.rotation },
                  d.options.animation
                ));
            }),
            (d.animate = null));
        },
        render: function () {
          this.group = this.plotGroup(
            "group",
            "series",
            this.visible ? "visible" : "hidden",
            this.options.zIndex,
            this.chart.seriesGroup
          );
          e.prototype.render.call(this);
          this.group.clip(this.chart.clipRect);
        },
        setData: function (a, d) {
          e.prototype.setData.call(this, a, !1);
          this.processData();
          this.generatePoints();
          m(d, !0) && this.chart.redraw();
        },
        drawTracker: d && d.drawTrackerPoint,
      },
      {
        setState: function (a) {
          this.state = a;
        },
      }
    );
  })(x);
  (function (b) {
    var r = b.each,
      t = b.noop,
      w = b.pick,
      m = b.seriesType,
      q = b.seriesTypes;
    m(
      "boxplot",
      "column",
      {
        threshold: null,
        tooltip: {
          pointFormat:
            '\x3cspan style\x3d"color:{point.color}"\x3e\u25cf\x3c/span\x3e \x3cb\x3e {series.name}\x3c/b\x3e\x3cbr/\x3eMaximum: {point.high}\x3cbr/\x3eUpper quartile: {point.q3}\x3cbr/\x3eMedian: {point.median}\x3cbr/\x3eLower quartile: {point.q1}\x3cbr/\x3eMinimum: {point.low}\x3cbr/\x3e',
        },
        whiskerLength: "50%",
        fillColor: "#ffffff",
        lineWidth: 1,
        medianWidth: 2,
        states: { hover: { brightness: -0.3 } },
        whiskerWidth: 2,
      },
      {
        pointArrayMap: ["low", "q1", "median", "q3", "high"],
        toYData: function (b) {
          return [b.low, b.q1, b.median, b.q3, b.high];
        },
        pointValKey: "high",
        pointAttribs: function (b) {
          var a = this.options,
            d = (b && b.color) || this.color;
          return {
            fill: b.fillColor || a.fillColor || d,
            stroke: a.lineColor || d,
            "stroke-width": a.lineWidth || 0,
          };
        },
        drawDataLabels: t,
        translate: function () {
          var b = this.yAxis,
            a = this.pointArrayMap;
          q.column.prototype.translate.apply(this);
          r(this.points, function (d) {
            r(a, function (a) {
              null !== d[a] && (d[a + "Plot"] = b.translate(d[a], 0, 1, 0, 1));
            });
          });
        },
        drawPoints: function () {
          var b = this,
            a = b.options,
            d = b.chart.renderer,
            f,
            h,
            u,
            c,
            l,
            k,
            p = 0,
            g,
            n,
            m,
            q,
            v = !1 !== b.doQuartiles,
            t,
            x = b.options.whiskerLength;
          r(b.points, function (e) {
            var r = e.graphic,
              y = r ? "animate" : "attr",
              I = e.shapeArgs,
              z = {},
              B = {},
              G = {},
              H = e.color || b.color;
            void 0 !== e.plotY &&
              ((g = I.width),
              (n = Math.floor(I.x)),
              (m = n + g),
              (q = Math.round(g / 2)),
              (f = Math.floor(v ? e.q1Plot : e.lowPlot)),
              (h = Math.floor(v ? e.q3Plot : e.lowPlot)),
              (u = Math.floor(e.highPlot)),
              (c = Math.floor(e.lowPlot)),
              r ||
                ((e.graphic = r = d.g("point").add(b.group)),
                (e.stem = d.path().addClass("highcharts-boxplot-stem").add(r)),
                x &&
                  (e.whiskers = d
                    .path()
                    .addClass("highcharts-boxplot-whisker")
                    .add(r)),
                v &&
                  (e.box = d
                    .path(void 0)
                    .addClass("highcharts-boxplot-box")
                    .add(r)),
                (e.medianShape = d
                  .path(void 0)
                  .addClass("highcharts-boxplot-median")
                  .add(r)),
                (z.stroke = e.stemColor || a.stemColor || H),
                (z["stroke-width"] = w(e.stemWidth, a.stemWidth, a.lineWidth)),
                (z.dashstyle = e.stemDashStyle || a.stemDashStyle),
                e.stem.attr(z),
                x &&
                  ((B.stroke = e.whiskerColor || a.whiskerColor || H),
                  (B["stroke-width"] = w(
                    e.whiskerWidth,
                    a.whiskerWidth,
                    a.lineWidth
                  )),
                  e.whiskers.attr(B)),
                v && ((r = b.pointAttribs(e)), e.box.attr(r)),
                (G.stroke = e.medianColor || a.medianColor || H),
                (G["stroke-width"] = w(
                  e.medianWidth,
                  a.medianWidth,
                  a.lineWidth
                )),
                e.medianShape.attr(G)),
              (k = (e.stem.strokeWidth() % 2) / 2),
              (p = n + q + k),
              e.stem[y]({ d: ["M", p, h, "L", p, u, "M", p, f, "L", p, c] }),
              v &&
                ((k = (e.box.strokeWidth() % 2) / 2),
                (f = Math.floor(f) + k),
                (h = Math.floor(h) + k),
                (n += k),
                (m += k),
                e.box[y]({
                  d: [
                    "M",
                    n,
                    h,
                    "L",
                    n,
                    f,
                    "L",
                    m,
                    f,
                    "L",
                    m,
                    h,
                    "L",
                    n,
                    h,
                    "z",
                  ],
                })),
              x &&
                ((k = (e.whiskers.strokeWidth() % 2) / 2),
                (u += k),
                (c += k),
                (t = /%$/.test(x) ? (q * parseFloat(x)) / 100 : x / 2),
                e.whiskers[y]({
                  d: [
                    "M",
                    p - t,
                    u,
                    "L",
                    p + t,
                    u,
                    "M",
                    p - t,
                    c,
                    "L",
                    p + t,
                    c,
                  ],
                })),
              (l = Math.round(e.medianPlot)),
              (k = (e.medianShape.strokeWidth() % 2) / 2),
              (l += k),
              e.medianShape[y]({ d: ["M", n, l, "L", m, l] }));
          });
        },
        setStackedPoints: t,
      }
    );
  })(x);
  (function (b) {
    var r = b.each,
      t = b.noop,
      w = b.seriesType,
      m = b.seriesTypes;
    w(
      "errorbar",
      "boxplot",
      {
        color: "#000000",
        grouping: !1,
        linkedTo: ":previous",
        tooltip: {
          pointFormat:
            '\x3cspan style\x3d"color:{point.color}"\x3e\u25cf\x3c/span\x3e {series.name}: \x3cb\x3e{point.low}\x3c/b\x3e - \x3cb\x3e{point.high}\x3c/b\x3e\x3cbr/\x3e',
        },
        whiskerWidth: null,
      },
      {
        type: "errorbar",
        pointArrayMap: ["low", "high"],
        toYData: function (b) {
          return [b.low, b.high];
        },
        pointValKey: "high",
        doQuartiles: !1,
        drawDataLabels: m.arearange
          ? function () {
              var b = this.pointValKey;
              m.arearange.prototype.drawDataLabels.call(this);
              r(this.data, function (e) {
                e.y = e[b];
              });
            }
          : t,
        getColumnMetrics: function () {
          return (
            (this.linkedParent && this.linkedParent.columnMetrics) ||
            m.column.prototype.getColumnMetrics.call(this)
          );
        },
      }
    );
  })(x);
  (function (b) {
    var r = b.correctFloat,
      t = b.isNumber,
      w = b.pick,
      m = b.Point,
      q = b.Series,
      e = b.seriesType,
      a = b.seriesTypes;
    e(
      "waterfall",
      "column",
      {
        dataLabels: { inside: !0 },
        lineWidth: 1,
        lineColor: "#333333",
        dashStyle: "dot",
        borderColor: "#333333",
        states: { hover: { lineWidthPlus: 0 } },
      },
      {
        pointValKey: "y",
        translate: function () {
          var d = this.options,
            b = this.yAxis,
            h,
            e,
            c,
            l,
            k,
            p,
            g,
            n,
            m,
            q = w(d.minPointLength, 5),
            v = d.threshold,
            t = d.stacking;
          a.column.prototype.translate.apply(this);
          this.minPointLengthOffset = 0;
          g = n = v;
          e = this.points;
          h = 0;
          for (d = e.length; h < d; h++)
            (c = e[h]),
              (p = this.processedYData[h]),
              (l = c.shapeArgs),
              (m = (k =
                t &&
                b.stacks[(this.negStacks && p < v ? "-" : "") + this.stackKey])
                ? k[c.x].points[this.index + "," + h]
                : [0, p]),
              c.isSum ? (c.y = r(p)) : c.isIntermediateSum && (c.y = r(p - n)),
              (k = Math.max(g, g + c.y) + m[0]),
              (l.y = b.toPixels(k, !0)),
              c.isSum
                ? ((l.y = b.toPixels(m[1], !0)),
                  (l.height =
                    Math.min(b.toPixels(m[0], !0), b.len) -
                    l.y +
                    this.minPointLengthOffset))
                : c.isIntermediateSum
                ? ((l.y = b.toPixels(m[1], !0)),
                  (l.height =
                    Math.min(b.toPixels(n, !0), b.len) -
                    l.y +
                    this.minPointLengthOffset),
                  (n = m[1]))
                : ((l.height =
                    0 < p
                      ? b.toPixels(g, !0) - l.y
                      : b.toPixels(g, !0) - b.toPixels(g - p, !0)),
                  (g += p)),
              0 > l.height && ((l.y += l.height), (l.height *= -1)),
              (c.plotY = l.y = Math.round(l.y) - (this.borderWidth % 2) / 2),
              (l.height = Math.max(Math.round(l.height), 0.001)),
              (c.yBottom = l.y + l.height),
              l.height <= q &&
                ((l.height = q), (this.minPointLengthOffset += q)),
              (l.y -= this.minPointLengthOffset),
              (l =
                c.plotY +
                (c.negative ? l.height : 0) -
                this.minPointLengthOffset),
              this.chart.inverted
                ? (c.tooltipPos[0] = b.len - l)
                : (c.tooltipPos[1] = l);
        },
        processData: function (a) {
          var b = this.yData,
            d = this.options.data,
            e,
            c = b.length,
            l,
            k,
            p,
            g,
            n,
            m;
          k = l = p = g = this.options.threshold || 0;
          for (m = 0; m < c; m++)
            (n = b[m]),
              (e = d && d[m] ? d[m] : {}),
              "sum" === n || e.isSum
                ? (b[m] = r(k))
                : "intermediateSum" === n || e.isIntermediateSum
                ? (b[m] = r(l))
                : ((k += n), (l += n)),
              (p = Math.min(k, p)),
              (g = Math.max(k, g));
          q.prototype.processData.call(this, a);
          this.dataMin = p;
          this.dataMax = g;
        },
        toYData: function (a) {
          return a.isSum
            ? 0 === a.x
              ? null
              : "sum"
            : a.isIntermediateSum
            ? 0 === a.x
              ? null
              : "intermediateSum"
            : a.y;
        },
        pointAttribs: function (b, f) {
          var d = this.options.upColor;
          d && !b.options.color && (b.color = 0 < b.y ? d : null);
          b = a.column.prototype.pointAttribs.call(this, b, f);
          delete b.dashstyle;
          return b;
        },
        getGraphPath: function () {
          return ["M", 0, 0];
        },
        getCrispPath: function () {
          var a = this.data,
            b = a.length,
            h = this.graph.strokeWidth() + this.borderWidth,
            h = (Math.round(h) % 2) / 2,
            e = [],
            c,
            l,
            k;
          for (k = 1; k < b; k++)
            (l = a[k].shapeArgs),
              (c = a[k - 1].shapeArgs),
              (l = ["M", c.x + c.width, c.y + h, "L", l.x, c.y + h]),
              0 > a[k - 1].y && ((l[2] += c.height), (l[5] += c.height)),
              (e = e.concat(l));
          return e;
        },
        drawGraph: function () {
          q.prototype.drawGraph.call(this);
          this.graph.attr({ d: this.getCrispPath() });
        },
        getExtremes: b.noop,
      },
      {
        getClassName: function () {
          var a = m.prototype.getClassName.call(this);
          this.isSum
            ? (a += " highcharts-sum")
            : this.isIntermediateSum && (a += " highcharts-intermediate-sum");
          return a;
        },
        isValid: function () {
          return t(this.y, !0) || this.isSum || this.isIntermediateSum;
        },
      }
    );
  })(x);
  (function (b) {
    var r = b.Series,
      t = b.seriesType,
      w = b.seriesTypes;
    t(
      "polygon",
      "scatter",
      {
        marker: { enabled: !1, states: { hover: { enabled: !1 } } },
        stickyTracking: !1,
        tooltip: { followPointer: !0, pointFormat: "" },
        trackByArea: !0,
      },
      {
        type: "polygon",
        getGraphPath: function () {
          for (
            var b = r.prototype.getGraphPath.call(this), q = b.length + 1;
            q--;

          )
            (q === b.length || "M" === b[q]) && 0 < q && b.splice(q, 0, "z");
          return (this.areaPath = b);
        },
        drawGraph: function () {
          this.options.fillColor = this.color;
          w.area.prototype.drawGraph.call(this);
        },
        drawLegendSymbol: b.LegendSymbolMixin.drawRectangle,
        drawTracker: r.prototype.drawTracker,
        setStackedPoints: b.noop,
      }
    );
  })(x);
  (function (b) {
    var r = b.arrayMax,
      t = b.arrayMin,
      w = b.Axis,
      m = b.color,
      q = b.each,
      e = b.isNumber,
      a = b.noop,
      d = b.pick,
      f = b.pInt,
      h = b.Point,
      u = b.Series,
      c = b.seriesType,
      l = b.seriesTypes;
    c(
      "bubble",
      "scatter",
      {
        dataLabels: {
          formatter: function () {
            return this.point.z;
          },
          inside: !0,
          verticalAlign: "middle",
        },
        marker: {
          lineColor: null,
          lineWidth: 1,
          radius: null,
          states: { hover: { radiusPlus: 0 } },
        },
        minSize: 8,
        maxSize: "20%",
        softThreshold: !1,
        states: { hover: { halo: { size: 5 } } },
        tooltip: { pointFormat: "({point.x}, {point.y}), Size: {point.z}" },
        turboThreshold: 0,
        zThreshold: 0,
        zoneAxis: "z",
      },
      {
        pointArrayMap: ["y", "z"],
        parallelArrays: ["x", "y", "z"],
        trackerGroups: ["group", "dataLabelsGroup"],
        bubblePadding: !0,
        zoneAxis: "z",
        markerAttribs: null,
        pointAttribs: function (a, b) {
          var c = d(this.options.marker.fillOpacity, 0.5);
          a = u.prototype.pointAttribs.call(this, a, b);
          1 !== c && (a.fill = m(a.fill).setOpacity(c).get("rgba"));
          return a;
        },
        getRadii: function (a, b, d, c) {
          var g,
            k,
            f,
            h = this.zData,
            e = [],
            l = this.options,
            n = "width" !== l.sizeBy,
            p = l.zThreshold,
            u = b - a;
          k = 0;
          for (g = h.length; k < g; k++)
            (f = h[k]),
              l.sizeByAbsoluteValue &&
                null !== f &&
                ((f = Math.abs(f - p)),
                (b = Math.max(b - p, Math.abs(a - p))),
                (a = 0)),
              null === f
                ? (f = null)
                : f < a
                ? (f = d / 2 - 1)
                : ((f = 0 < u ? (f - a) / u : 0.5),
                  n && 0 <= f && (f = Math.sqrt(f)),
                  (f = Math.ceil(d + f * (c - d)) / 2)),
              e.push(f);
          this.radii = e;
        },
        animate: function (a) {
          var b = this.options.animation;
          a ||
            (q(this.points, function (a) {
              var d = a.graphic;
              a = a.shapeArgs;
              d && a && (d.attr("r", 1), d.animate({ r: a.r }, b));
            }),
            (this.animate = null));
        },
        translate: function () {
          var a,
            b = this.data,
            d,
            c,
            f = this.radii;
          l.scatter.prototype.translate.call(this);
          for (a = b.length; a--; )
            (d = b[a]),
              (c = f ? f[a] : 0),
              e(c) && c >= this.minPxSize / 2
                ? ((d.shapeType = "circle"),
                  (d.shapeArgs = { x: d.plotX, y: d.plotY, r: c }),
                  (d.dlBox = {
                    x: d.plotX - c,
                    y: d.plotY - c,
                    width: 2 * c,
                    height: 2 * c,
                  }))
                : (d.shapeArgs = d.plotY = d.dlBox = void 0);
        },
        drawLegendSymbol: function (a, b) {
          var d = this.chart.renderer,
            c = d.fontMetrics(a.itemStyle.fontSize).f / 2;
          b.legendSymbol = d
            .circle(c, a.baseline - c, c)
            .attr({ zIndex: 3 })
            .add(b.legendGroup);
          b.legendSymbol.isMarker = !0;
        },
        drawPoints: l.column.prototype.drawPoints,
        alignDataLabel: l.column.prototype.alignDataLabel,
        buildKDTree: a,
        applyZones: a,
      },
      {
        haloPath: function (a) {
          return h.prototype.haloPath.call(this, this.shapeArgs.r + a);
        },
        ttBelow: !1,
      }
    );
    w.prototype.beforePadding = function () {
      var a = this,
        b = this.len,
        c = this.chart,
        h = 0,
        l = b,
        u = this.isXAxis,
        m = u ? "xData" : "yData",
        w = this.min,
        x = {},
        A = Math.min(c.plotWidth, c.plotHeight),
        C = Number.MAX_VALUE,
        D = -Number.MAX_VALUE,
        E = this.max - w,
        z = b / E,
        F = [];
      q(this.series, function (b) {
        var g = b.options;
        !b.bubblePadding ||
          (!b.visible && c.options.chart.ignoreHiddenSeries) ||
          ((a.allowZoomOutside = !0),
          F.push(b),
          u &&
            (q(["minSize", "maxSize"], function (a) {
              var b = g[a],
                d = /%$/.test(b),
                b = f(b);
              x[a] = d ? (A * b) / 100 : b;
            }),
            (b.minPxSize = x.minSize),
            (b.maxPxSize = Math.max(x.maxSize, x.minSize)),
            (b = b.zData),
            b.length &&
              ((C = d(
                g.zMin,
                Math.min(
                  C,
                  Math.max(
                    t(b),
                    !1 === g.displayNegative ? g.zThreshold : -Number.MAX_VALUE
                  )
                )
              )),
              (D = d(g.zMax, Math.max(D, r(b)))))));
      });
      q(F, function (b) {
        var d = b[m],
          c = d.length,
          f;
        u && b.getRadii(C, D, b.minPxSize, b.maxPxSize);
        if (0 < E)
          for (; c--; )
            e(d[c]) &&
              a.dataMin <= d[c] &&
              d[c] <= a.dataMax &&
              ((f = b.radii[c]),
              (h = Math.min((d[c] - w) * z - f, h)),
              (l = Math.max((d[c] - w) * z + f, l)));
      });
      F.length &&
        0 < E &&
        !this.isLog &&
        ((l -= b),
        (z *= (b + h - l) / b),
        q(
          [
            ["min", "userMin", h],
            ["max", "userMax", l],
          ],
          function (b) {
            void 0 === d(a.options[b[0]], a[b[1]]) && (a[b[0]] += b[2] / z);
          }
        ));
    };
  })(x);
  (function (b) {
    function r(a, b) {
      var d = this.chart,
        h = this.options.animation,
        e = this.group,
        c = this.markerGroup,
        l = this.xAxis.center,
        k = d.plotLeft,
        p = d.plotTop;
      d.polar
        ? d.renderer.isSVG &&
          (!0 === h && (h = {}),
          b
            ? ((a = {
                translateX: l[0] + k,
                translateY: l[1] + p,
                scaleX: 0.001,
                scaleY: 0.001,
              }),
              e.attr(a),
              c && c.attr(a))
            : ((a = { translateX: k, translateY: p, scaleX: 1, scaleY: 1 }),
              e.animate(a, h),
              c && c.animate(a, h),
              (this.animate = null)))
        : a.call(this, b);
    }
    var t = b.each,
      w = b.pick,
      m = b.seriesTypes,
      q = b.wrap,
      e = b.Series.prototype;
    b = b.Pointer.prototype;
    e.searchPointByAngle = function (a) {
      var b = this.chart,
        f = this.xAxis.pane.center;
      return this.searchKDTree({
        clientX:
          180 +
          (-180 / Math.PI) *
            Math.atan2(
              a.chartX - f[0] - b.plotLeft,
              a.chartY - f[1] - b.plotTop
            ),
      });
    };
    q(e, "buildKDTree", function (a) {
      this.chart.polar &&
        (this.kdByAngle
          ? (this.searchPoint = this.searchPointByAngle)
          : (this.kdDimensions = 2));
      a.apply(this);
    });
    e.toXY = function (a) {
      var b,
        f = this.chart,
        h = a.plotX;
      b = a.plotY;
      a.rectPlotX = h;
      a.rectPlotY = b;
      b = this.xAxis.postTranslate(a.plotX, this.yAxis.len - b);
      a.plotX = a.polarPlotX = b.x - f.plotLeft;
      a.plotY = a.polarPlotY = b.y - f.plotTop;
      this.kdByAngle
        ? ((f =
            ((h / Math.PI) * 180 + this.xAxis.pane.options.startAngle) % 360),
          0 > f && (f += 360),
          (a.clientX = f))
        : (a.clientX = a.plotX);
    };
    m.spline &&
      q(m.spline.prototype, "getPointSpline", function (a, b, f, h) {
        var d, c, e, k, p, g, n;
        this.chart.polar
          ? ((d = f.plotX),
            (c = f.plotY),
            (a = b[h - 1]),
            (e = b[h + 1]),
            this.connectEnds && (a || (a = b[b.length - 2]), e || (e = b[1])),
            a &&
              e &&
              ((k = a.plotX),
              (p = a.plotY),
              (b = e.plotX),
              (g = e.plotY),
              (k = (1.5 * d + k) / 2.5),
              (p = (1.5 * c + p) / 2.5),
              (e = (1.5 * d + b) / 2.5),
              (n = (1.5 * c + g) / 2.5),
              (b = Math.sqrt(Math.pow(k - d, 2) + Math.pow(p - c, 2))),
              (g = Math.sqrt(Math.pow(e - d, 2) + Math.pow(n - c, 2))),
              (k = Math.atan2(p - c, k - d)),
              (p = Math.atan2(n - c, e - d)),
              (n = Math.PI / 2 + (k + p) / 2),
              Math.abs(k - n) > Math.PI / 2 && (n -= Math.PI),
              (k = d + Math.cos(n) * b),
              (p = c + Math.sin(n) * b),
              (e = d + Math.cos(Math.PI + n) * g),
              (n = c + Math.sin(Math.PI + n) * g),
              (f.rightContX = e),
              (f.rightContY = n)),
            h
              ? ((f = [
                  "C",
                  a.rightContX || a.plotX,
                  a.rightContY || a.plotY,
                  k || d,
                  p || c,
                  d,
                  c,
                ]),
                (a.rightContX = a.rightContY = null))
              : (f = ["M", d, c]))
          : (f = a.call(this, b, f, h));
        return f;
      });
    q(e, "translate", function (a) {
      var b = this.chart;
      a.call(this);
      if (
        b.polar &&
        ((this.kdByAngle = b.tooltip && b.tooltip.shared),
        !this.preventPostTranslate)
      )
        for (a = this.points, b = a.length; b--; ) this.toXY(a[b]);
    });
    q(e, "getGraphPath", function (a, b) {
      var d = this,
        e,
        m;
      if (this.chart.polar) {
        b = b || this.points;
        for (e = 0; e < b.length; e++)
          if (!b[e].isNull) {
            m = e;
            break;
          }
        !1 !== this.options.connectEnds &&
          void 0 !== m &&
          ((this.connectEnds = !0), b.splice(b.length, 0, b[m]));
        t(b, function (a) {
          void 0 === a.polarPlotY && d.toXY(a);
        });
      }
      return a.apply(this, [].slice.call(arguments, 1));
    });
    q(e, "animate", r);
    m.column &&
      ((m = m.column.prototype),
      (m.polarArc = function (a, b, e, h) {
        var d = this.xAxis.center,
          c = this.yAxis.len;
        return this.chart.renderer.symbols.arc(d[0], d[1], c - b, null, {
          start: e,
          end: h,
          innerR: c - w(a, c),
        });
      }),
      q(m, "animate", r),
      q(m, "translate", function (a) {
        var b = this.xAxis,
          e = b.startAngleRad,
          h,
          m,
          c;
        this.preventPostTranslate = !0;
        a.call(this);
        if (b.isRadial)
          for (h = this.points, c = h.length; c--; )
            (m = h[c]),
              (a = m.barX + e),
              (m.shapeType = "path"),
              (m.shapeArgs = {
                d: this.polarArc(m.yBottom, m.plotY, a, a + m.pointWidth),
              }),
              this.toXY(m),
              (m.tooltipPos = [m.plotX, m.plotY]),
              (m.ttBelow = m.plotY > b.center[1]);
      }),
      q(m, "alignDataLabel", function (a, b, f, h, m, c) {
        this.chart.polar
          ? ((a = (b.rectPlotX / Math.PI) * 180),
            null === h.align &&
              (h.align =
                20 < a && 160 > a
                  ? "left"
                  : 200 < a && 340 > a
                  ? "right"
                  : "center"),
            null === h.verticalAlign &&
              (h.verticalAlign =
                45 > a || 315 < a
                  ? "bottom"
                  : 135 < a && 225 > a
                  ? "top"
                  : "middle"),
            e.alignDataLabel.call(this, b, f, h, m, c))
          : a.call(this, b, f, h, m, c);
      }));
    q(b, "getCoordinates", function (a, b) {
      var d = this.chart,
        e = { xAxis: [], yAxis: [] };
      d.polar
        ? t(d.axes, function (a) {
            var c = a.isXAxis,
              f = a.center,
              h = b.chartX - f[0] - d.plotLeft,
              f = b.chartY - f[1] - d.plotTop;
            e[c ? "xAxis" : "yAxis"].push({
              axis: a,
              value: a.translate(
                c
                  ? Math.PI - Math.atan2(h, f)
                  : Math.sqrt(Math.pow(h, 2) + Math.pow(f, 2)),
                !0
              ),
            });
          })
        : (e = a.call(this, b));
      return e;
    });
  })(x);
});
