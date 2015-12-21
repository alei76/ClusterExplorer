/*
 * This file is part of the OpenSphere project which aims to
 * develop geospatial algorithms.
 * 
 * Copyright (C) 2012 Eric Grosso
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * For more information, contact:
 * Eric Grosso, eric.grosso.os@gmail.com
 * 
 */
package org.bdigital.mob.c2020.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bdigital.mob.c2020.geo.DoubleComparator;
import org.bdigital.mob.c2020.geo.Edge;
import org.bdigital.mob.c2020.geo.Triangle;
import org.bdigital.mob.c2020.geo.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeTriangle;
import com.vividsolutions.jts.util.UniqueCoordinateArrayFilter;

// TODO: Auto-generated Javadoc
/**
 * Computes a concave hull of a {@link Geometry} which is a concave.
 * 
 * {@link Geometry} that contains all the points in the input {@link Geometry}.
 * The concave hull is not be defined as unique; here, it is defined according
 * to a threshold which is the maximum length of border edges of the concave
 * hull.
 * 
 * <p>
 * Uses the Duckham and al. (2008) algorithm defined in the paper untitled
 * "Efficient generation of simple polygons for characterizing the shape of a
 * set of points in the plane".
 * 
 * @author Eric Grosso
 */
public class ConcaveHull {

	/** The geom factory. */
	private final GeometryFactory geomFactory;

	/** The geometries. */
	private final GeometryCollection geometries;

	/** The threshold. */
	private final double threshold;

	/** The segments. */
	public HashMap<LineSegment, Integer> segments = new HashMap<LineSegment, Integer>();

	/** The edges. */
	public HashMap<Integer, Edge> edges = new HashMap<Integer, Edge>();

	/** The triangles. */
	public HashMap<Integer, Triangle> triangles = new HashMap<Integer, Triangle>();

	/** The lengths. */
	public TreeMap<Integer, Edge> lengths = new TreeMap<Integer, Edge>();

	/** The short lengths. */
	public HashMap<Integer, Edge> shortLengths = new HashMap<Integer, Edge>();

	/** The coordinates. */
	public HashMap<Coordinate, Integer> coordinates = new HashMap<Coordinate, Integer>();

	/** The vertices. */
	public HashMap<Integer, Vertex> vertices = new HashMap<Integer, Vertex>();

	/**
	 * Create a new concave hull construction for the input {@link Geometry}.
	 * 
	 * @param geometry
	 *            the geometry
	 * @param threshold
	 *            the threshold
	 */
	public ConcaveHull(final Geometry geometry, final double threshold) {
		geometries = transformIntoPointGeometryCollection(geometry);
		this.threshold = threshold;
		geomFactory = geometry.getFactory();
	}

	/**
	 * Create a new concave hull construction for the input.
	 * 
	 * @param geometries
	 *            the geometries
	 * @param threshold
	 *            the threshold {@link GeometryCollection}.
	 */
	public ConcaveHull(final GeometryCollection geometries,
			final double threshold) {
		this.geometries = transformIntoPointGeometryCollection(geometries);
		this.threshold = threshold;
		geomFactory = geometries.getFactory();
	}

	/**
	 * Transform into GeometryCollection.
	 * 
	 * @param geom
	 *            input geometry
	 * @return a geometry collection
	 */
	private static GeometryCollection transformIntoPointGeometryCollection(
			final Geometry geom) {
		final UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
		geom.apply(filter);
		final Coordinate[] coord = filter.getCoordinates();

		final Geometry[] geometries = new Geometry[coord.length];
		for (int i = 0; i < coord.length; i++) {
			final Coordinate[] c = new Coordinate[] { coord[i] };
			final CoordinateArraySequence cs = new CoordinateArraySequence(c);
			geometries[i] = new Point(cs, geom.getFactory());
		}

		return new GeometryCollection(geometries, geom.getFactory());
	}

	/**
	 * Transform into GeometryCollection.
	 * 
	 * @param gc
	 *            the gc
	 * @return a geometry collection
	 */
	private static GeometryCollection transformIntoPointGeometryCollection(
			final GeometryCollection gc) {
		final UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
		gc.apply(filter);
		final Coordinate[] coord = filter.getCoordinates();

		final Geometry[] geometries = new Geometry[coord.length];
		for (int i = 0; i < coord.length; i++) {
			final Coordinate[] c = new Coordinate[] { coord[i] };
			final CoordinateArraySequence cs = new CoordinateArraySequence(c);
			geometries[i] = new Point(cs, gc.getFactory());
		}

		return new GeometryCollection(geometries, gc.getFactory());
	}

	/**
	 * Returns a {@link Geometry} that represents the concave hull of the input
	 * geometry according to the threshold. The returned geometry contains the
	 * minimal number of points needed to represent the concave hull.
	 * 
	 * @return if the concave hull contains 3 or more points, a {@link Polygon};
	 *         2 points, a {@link LineString}; 1 point, a {@link Point}; 0
	 *         points, an empty {@link GeometryCollection}.
	 */
	public Geometry getConcaveHull() {

		if (geometries.getNumGeometries() == 0) {
			return geomFactory.createGeometryCollection(null);
		}
		if (geometries.getNumGeometries() == 1) {
			return geometries.getGeometryN(0);
		}
		if (geometries.getNumGeometries() == 2) {
			return geomFactory.createLineString(geometries.getCoordinates());
		}

		return concaveHull();
	}

	/**
	 * Create the concave hull.
	 * 
	 * @return the concave hull
	 */
	private Geometry concaveHull() {

		// triangulation: create a DelaunayTriangulationBuilder object
		final ConformingDelaunayTriangulationBuilder cdtb = new ConformingDelaunayTriangulationBuilder();

		// add geometry collection
		cdtb.setSites(geometries);

		final QuadEdgeSubdivision qes = cdtb.getSubdivision();

		final Collection<QuadEdge> quadEdges = qes.getEdges();
		final List<QuadEdgeTriangle> qeTriangles = QuadEdgeTriangle.createOn(qes);
		
		final Collection<com.vividsolutions.jts.triangulate.quadedge.Vertex> qeVertices = qes
				.getVertices(false);

		int iV = 0;
		for (final com.vividsolutions.jts.triangulate.quadedge.Vertex v : qeVertices) {
			coordinates.put(v.getCoordinate(), iV);
			vertices.put(iV, new Vertex(iV, v.getCoordinate()));
			iV++;
		}

		// border
		final List<QuadEdge> qeFrameBorder = new ArrayList<QuadEdge>();
		final List<QuadEdge> qeFrame = new ArrayList<QuadEdge>();
		final List<QuadEdge> qeBorder = new ArrayList<QuadEdge>();

		for (final QuadEdge qe : quadEdges) {
			if (qes.isFrameBorderEdge(qe)) {
				qeFrameBorder.add(qe);
			}
			if (qes.isFrameEdge(qe)) {
				qeFrame.add(qe);
			}
		}

		// border
		for (int j = 0; j < qeFrameBorder.size(); j++) {
			final QuadEdge q = qeFrameBorder.get(j);
			if (!qeFrame.contains(q)) {
				qeBorder.add(q);
			}
		}

		// deletion of exterior edges
		for (final QuadEdge qe : qeFrame) {
			qes.delete(qe);
		}

		final HashMap<QuadEdge, Double> qeDistances = new HashMap<QuadEdge, Double>();
		for (final QuadEdge qe : quadEdges) {
			qeDistances.put(qe, qe.toLineSegment().getLength());
		}

		final DoubleComparator dc = new DoubleComparator(qeDistances);
		final TreeMap<QuadEdge, Double> qeSorted = new TreeMap<QuadEdge, Double>(
				dc);
		qeSorted.putAll(qeDistances);

		// edges creation
		int i = 0;
		for (final QuadEdge qe : qeSorted.keySet()) {
			final LineSegment s = qe.toLineSegment();
			s.normalize();

			final Integer idS = coordinates.get(s.p0);
			final Integer idD = coordinates.get(s.p1);
			final Vertex oV = vertices.get(idS);
			final Vertex eV = vertices.get(idD);

			Edge edge;
			if (qeBorder.contains(qe)) {
				oV.setBorder(true);
				eV.setBorder(true);
				edge = new Edge(i, s, oV, eV, true);
				if (s.getLength() < threshold) {
					shortLengths.put(i, edge);
				} else {
					lengths.put(i, edge);
				}
			} else {
				edge = new Edge(i, s, oV, eV, false);
			}
			edges.put(i, edge);
			segments.put(s, i);
			i++;
		}

		// hm of linesegment and hm of edges // with id as key
		// hm of triangles using hm of ls and connection with hm of edges

		i = 0;
		for (final QuadEdgeTriangle qet : qeTriangles) {
			final LineSegment sA = qet.getEdge(0).toLineSegment();
			final LineSegment sB = qet.getEdge(1).toLineSegment();
			final LineSegment sC = qet.getEdge(2).toLineSegment();
			sA.normalize();
			sB.normalize();
			sC.normalize();

			final Edge edgeA = edges.get(segments.get(sA));
			final Edge edgeB = edges.get(segments.get(sB));
			final Edge edgeC = edges.get(segments.get(sC));

			final Triangle triangle = new Triangle(i, qet.isBorder() ? true
					: false);
			triangle.addEdge(edgeA);
			triangle.addEdge(edgeB);
			triangle.addEdge(edgeC);

			edgeA.addTriangle(triangle);
			edgeB.addTriangle(triangle);
			edgeC.addTriangle(triangle);

			triangles.put(i, triangle);
			i++;
		}

		// add triangle neighbourood
		for (final Edge edge : edges.values()) {
			if (edge.getTriangles().size() > 1) {
				final Triangle tA = edge.getTriangles().get(0);
				final Triangle tB = edge.getTriangles().get(1);
				tA.addNeighbour(tB);
				tB.addNeighbour(tA);
			}
		}

		// concave hull algorithm
		int index = 0;
		while (index != -1) {
			index = -1;

			Edge e = null;

			// find the max length (smallest id so first entry)
			final int si = lengths.size();

			if (si != 0) {
				final Entry<Integer, Edge> entry = lengths.firstEntry();
				final int ind = entry.getKey();
				if (entry.getValue().getGeometry().getLength() > threshold) {
					index = ind;
					e = entry.getValue();
				}
			}

			if ((index != -1) && (e.getTriangles().size() > 0)) {
				// if ((index != -1)) {
				final Triangle triangle = e.getTriangles().get(0);
				final List<Triangle> neighbours = triangle.getNeighbours();
				// irregular triangle test
				if (neighbours.size() == 1) {
					shortLengths.put(e.getId(), e);
					lengths.remove(e.getId());
				} else {
					final Edge e0 = triangle.getEdges().get(0);
					final Edge e1 = triangle.getEdges().get(1);
					// test if all the vertices are on the border
					if (e0.getOV().isBorder() && e0.getEV().isBorder()
							&& e1.getOV().isBorder() && e1.getEV().isBorder()) {
						shortLengths.put(e.getId(), e);
						lengths.remove(e.getId());
					} else {
						// management of triangles
						final Triangle tA = neighbours.get(0);
						final Triangle tB = neighbours.get(1);
						tA.setBorder(true); // FIXME not necessarily useful
						tB.setBorder(true); // FIXME not necessarily useful
						triangles.remove(triangle.getId());
						tA.removeNeighbour(triangle);
						tB.removeNeighbour(triangle);

						// new edges
						final List<Edge> ee = triangle.getEdges();
						final Edge eA = ee.get(0);
						final Edge eB = ee.get(1);
						final Edge eC = ee.get(2);

						if (eA.isBorder()) {
							edges.remove(eA.getId());
							eB.setBorder(true);
							eB.getOV().setBorder(true);
							eB.getEV().setBorder(true);
							eC.setBorder(true);
							eC.getOV().setBorder(true);
							eC.getEV().setBorder(true);

							// clean the relationships with the triangle
							eB.removeTriangle(triangle);
							eC.removeTriangle(triangle);

							if (eB.getGeometry().getLength() < threshold) {
								shortLengths.put(eB.getId(), eB);
							} else {
								lengths.put(eB.getId(), eB);
							}
							if (eC.getGeometry().getLength() < threshold) {
								shortLengths.put(eC.getId(), eC);
							} else {
								lengths.put(eC.getId(), eC);
							}
							lengths.remove(eA.getId());
						} else if (eB.isBorder()) {
							edges.remove(eB.getId());
							eA.setBorder(true);
							eA.getOV().setBorder(true);
							eA.getEV().setBorder(true);
							eC.setBorder(true);
							eC.getOV().setBorder(true);
							eC.getEV().setBorder(true);

							// clean the relationships with the triangle
							eA.removeTriangle(triangle);
							eC.removeTriangle(triangle);

							if (eA.getGeometry().getLength() < threshold) {
								shortLengths.put(eA.getId(), eA);
							} else {
								lengths.put(eA.getId(), eA);
							}
							if (eC.getGeometry().getLength() < threshold) {
								shortLengths.put(eC.getId(), eC);
							} else {
								lengths.put(eC.getId(), eC);
							}
							lengths.remove(eB.getId());
						} else {
							edges.remove(eC.getId());
							eA.setBorder(true);
							eA.getOV().setBorder(true);
							eA.getEV().setBorder(true);
							eB.setBorder(true);
							eB.getOV().setBorder(true);
							eB.getEV().setBorder(true);
							// clean the relationships with the triangle
							eA.removeTriangle(triangle);
							eB.removeTriangle(triangle);

							if (eA.getGeometry().getLength() < threshold) {
								shortLengths.put(eA.getId(), eA);
							} else {
								lengths.put(eA.getId(), eA);
							}
							if (eB.getGeometry().getLength() < threshold) {
								shortLengths.put(eB.getId(), eB);
							} else {
								lengths.put(eB.getId(), eB);
							}
							lengths.remove(eC.getId());
						}
					}
				}
			} else {
				break;
			}
		}

		// concave hull creation
		final List<LineString> edges = new ArrayList<LineString>();
		for (final Edge e : lengths.values()) {
			final LineString l = e.getGeometry().toGeometry(geomFactory);
			edges.add(l);
		}

		for (final Edge e : shortLengths.values()) {
			final LineString l = e.getGeometry().toGeometry(geomFactory);
			edges.add(l);
		}

		// merge
		final LineMerger lineMerger = new LineMerger();
		lineMerger.add(edges);
		final LineString merge = (LineString) lineMerger.getMergedLineStrings()
				.iterator().next();

		if (merge.isRing()) {
			final LinearRing lr = new LinearRing(merge.getCoordinateSequence(),
					geomFactory);
			final Polygon concaveHull = new Polygon(lr, null, geomFactory);
			return concaveHull;
		}

		return merge;
	}

}


