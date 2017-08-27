package com.flowtick.graphs.graphml

import scala.util._
import scala.xml.XML
import scala.collection.mutable

class GraphMLImporter {
  def fromXml(graphml: String): Either[Throwable, GraphMLGraph] =
    Try(XML.load(new java.io.ByteArrayInputStream(graphml.getBytes)))
      .filter(_.label.toLowerCase == "graphml").flatMap { rootElem =>
        rootElem.child.find(_.label.toLowerCase == "graph") match {
          case Some(graph) => Success(parseGraphNode(graph))
          case None => Failure(new IllegalArgumentException("graph node not found"))
        }
      }.toEither

  protected def singleAttributeValue(attributeName: String, node: scala.xml.Node): Option[String] = {
    node.attribute(attributeName).getOrElse(Seq.empty).headOption.map(_.text)
  }

  protected def parseGraphNode(graphNode: scala.xml.Node): GraphMLGraph = {
    GraphMLGraph.create(singleAttributeValue("id", graphNode).getOrElse("graph")) { implicit g =>
      val nodes = new mutable.HashMap[String, GraphMLNode]()
      val edges = new mutable.ListBuffer[scala.xml.Node]()

      graphNode.child.foreach {
        case node: scala.xml.Node if node.label == "node" =>
          val nodeId: String = singleAttributeValue("id", node).getOrElse("node")
          val graphNode = GraphMLNode(nodeId)
          nodes.put(nodeId, graphNode)
          g.addNode(graphNode)
        case edge: scala.xml.Node if edge.label == "edge" =>
          edges.append(edge)
        case _ =>
      }

      edges.foreach { e =>
        val edgeId = singleAttributeValue("id", e).getOrElse("edge")

        for {
          source <- singleAttributeValue("source", e)
          target <- singleAttributeValue("target", e)
          sourceNode <- nodes.get(source)
          targetNode <- nodes.get(target)
        } yield {
          val graphEdge = GraphMLEdge(
            edgeId,
            None,
            sourceNode,
            targetNode
          )
          g.addEdge(graphEdge)
        }
      }
    }
  }
}