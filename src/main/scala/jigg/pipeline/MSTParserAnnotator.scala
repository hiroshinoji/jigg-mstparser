package jigg.pipeline

/*
 Copyright 2013-2016 Hiroshi Noji

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/


import jigg.util.PropertiesUtil
import jigg.util.{IOUtil, XMLUtil}

import java.io.BufferedInputStream
import java.util.Properties

import scala.xml._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

import mstparser._

class MSTParserAnnotator(
  override val name: String,
  override val props: Properties) extends SentencesAnnotator with ParallelAnnotator {

  @Prop(gloss = "Path to model file") var modelName = ""
  @Prop(gloss = "Use the 2nd order features (This should match the setting of the loaded model)") var secondOrder = true
  @Prop(gloss = "Employ non-projective decoding") var nonproj = true

  readProps()

  lazy val parserQueue = new ResourceQueue(nThreads, mkParser _) {
    def postProcess(parser: Parser, e: ProcessError) = {
      queue.put(parser)
      throw e
    }
  }

  override def description = s"""${super.description}
  This parser is relatively the old package and currently not the state-of-the-art.
  Perhaps one advantage of using this parser instead of more recent systems, e.g., the Stanford parser, is that this parser can handle non-projective trees.

  The tool is still under construction. We are planning to release the pre-trained model files soon.

  The model is assumed to be trained from the file in MST format (rather than CoNLL format).
  This change slightly affects the behavior of the model.
"""

  override def init() = {
    parserQueue // init here, to output help message without loading
  }

  def mkParser(): Parser = {
    val opts = new ParserOptions(Array.empty[String])
    opts.modelName = modelName
    opts.decodeType = if (nonproj) "non-proj" else "proj"
    opts.secondOrder = secondOrder
    opts.format = "MST"

    val pipe = secondOrder match {
      case true => new DependencyPipe2O(opts)
      case false => new DependencyPipe(opts)
    }
    pipe.labeled = true

    val parser = new Parser(pipe, opts)

    System.err.println("Loading MSTParser model from " + modelName)
    val inStream = IOUtil.inStream(modelName)
    parser.loadModel(new BufferedInputStream(inStream))
    inStream.close
    pipe.closeAlphabets()

    parser
  }

  def newSentenceAnnotation(sentence: Node) = {

    val tokens = (sentence \ "tokens").head \ "token"
    val tokenIds = tokens map (_ \@ "id")

    val instance = sentenceToInstance(tokens)

    val Arcs(labels, heads) = parse(instance)

    val depNodes = (0 until heads.size) map { i =>
      val head = heads(i) match {
        case 0 => "ROOT"
        case h => tokenIds(h - 1)
      }
      val dep = tokenIds(i)
      val deprel = labels(i)
      <dependency id={Annotation.Dependency.nextId}
        head={ head } dep={ dep } deprel={ deprel }/>
    }
    val deps = <dependencies annotators={ name }>{ depNodes }</dependencies>

    XMLUtil.addChild(sentence, deps)
  }

  // We don't use depReader as it highly connects to the external IO
  def sentenceToInstance(tokens: NodeSeq): DependencyInstance = {

    val forms = "<root>" +: tokens.map(_ \@ "form").toArray
    val poses = "<root-POS>" +: tokens.map(_ \@ "pos").toArray
    val deprels = "<no-type>" +: Array.fill(tokens.size)("a")
    val heads = -1 +: Array.fill(tokens.size)(0)

    val instance = new DependencyInstance(
      forms, poses, deprels, heads, null)

    val cpostags = "<root-CPOS>" +: poses.tail.map(_.take(1))
    val lemmas = "<root-LEMMA>" +: forms.tail.map(_.take(5))

    instance.cpostags = cpostags
    instance.lemmas = lemmas
    instance.feats = Array.ofDim[String](0, 0)

    instance
  }

  def parse(instance: DependencyInstance): Arcs = parserQueue using { _ decode instance }

  class Parser(pipe: DependencyPipe, options: ParserOptions)
      extends DependencyParser(pipe, options) {

    def decode(instance: DependencyInstance): Arcs = {

      instance.setFeatureVector(pipe.createFeatureVector(instance))

      val forms = instance.forms
      val formsNoRoot = new Array[String](forms.size - 1)
      val posNoRoot = new Array[String](formsNoRoot.size)
      val labels = new Array[String](formsNoRoot.size)
      val heads = new Array[Int](formsNoRoot.size)

      val res = decode(instance, 1, params, formsNoRoot, posNoRoot, labels, heads)

      Arcs(labels, heads)
    }
  }

  case class Arcs(labels: Array[String], heads: Array[Int])

  override def requires = Set(Requirement.POS)
  override def requirementsSatisfied = Set(Requirement.BasicDependencies) // is it basic?
}
