package insane
package AST

import utils._
import GlobalCounters.withDebugCounter

trait CodeExtraction extends Extractors with Contracts {
  self: AnalysisComponent =>

  import global._

  import StructuralExtractors._

  class CodeExtractionPhase extends SubPhase {
    val name = "Extracting definitions and contracts"

    def run() {
      for (unit <- currentRun.units) {
        new ForeachTreeTraverser(traverseStep).traverse(unit.body)
      }

      reporter.msg("Found "+declaredFunctions.size+" methods to analyze...")
    }

    def extractStubInfo(fun: AbsFunction) {
      def readClassAnnotation(symbol: Symbol): Option[Symbol] = {
        symbol.annotations.find(_.atp.safeToString startsWith "insane.annotations.Abstracts") match {
            case Some(annot) =>

              annot.args match {
                case List(l: Literal) =>
                  val name = l.value.stringValue

                  try {
                    annot.atp.safeToString match {
                      case "insane.annotations.AbstractsClass" =>
                        Some(rootMirror.getClassIfDefined(name))
                      case "insane.annotations.AbstractsModuleClass" =>
                        Some(rootMirror.getModuleByName(newTypeName(name)).moduleClass)
                      case _ =>
                        reporter.error("Could not understand annotation: "+annot, Some(symbol.pos))
                        None
                    }
                  } catch {
                    case e: Throwable =>
                      reporter.error("Unable to find class symbol from name "+name+": "+e.getMessage, Some(symbol.pos))
                      None
                  }
                case _ =>
                  reporter.error("Could not understand annotation: "+annot, Some(symbol.pos))
                  None
              }
            case None =>
              None
          }
      }

      def readMethodAnnotation(symbol: Symbol): Option[Symbol] = {
        symbol.annotations.find(List("insane.annotations.AbstractsMethod", "insane.annotations.AbstractsStaticMethod") contains _.atp.safeToString) match {
            case Some(annot) =>

              annot.args match {
                case List(l: Literal) => 
                  val methDesc = l.value.stringValue
                  val (methFullName, sig) = methDesc.splitAt(methDesc.indexOf('('))
                  val (className, methStr) = methFullName.splitAt(methFullName.lastIndexOf('.'))

                  val cl = if (annot.atp.safeToString == "insane.annotations.AbstractsMethod") {
                    rootMirror.getClassIfDefined(className)
                  } else {
                    rootMirror.getModuleByName(newTypeName(className)).moduleClass
                  }

                  val methName = methStr.tail
                  val meth = definitions.termMember(cl, methName)

                  if (meth != NoSymbol) {
                    if (meth.isOverloaded) {
                      meth.alternatives.find(uniqueFunctionName(_) == methDesc)
                    } else {
                      Some(meth)
                    }
                  } else {
                    reporter.error("Could not find "+methName+" in "+cl.fullName+"..")
                    None
                  }
                case _ =>
                  reporter.error("Could not understand annotation: "+annot, Some(symbol.pos))
                  None
              }
            case None =>
              None
          }
      }

      (readMethodAnnotation(fun.symbol), readClassAnnotation(fun.symbol.owner)) match {
        case (Some(ms), Some(cs)) =>
          fun.implOfMethod = Some(ms)
          fun.implOfClass  = Some(cs)

          methodProxies.get(ms) match {
            case Some(other) =>
              reporter.error("Method "+ms.fullName+" is already being implemented by "+other.symbol.fullName+", cannot select "+fun.symbol.fullName+" as its second implementation")
            case None =>
              methodProxies += ms -> fun

          }
        case _ =>
          // No info found, or incomplete
      }
    }

    def extractEffectsAnotations(fun: AbsFunction) {
      val effectAnnotations = List("insane.annotations.WillNotModify",
                                   "insane.annotations.MayOnlyModify")

      fun.symbol.annotations.filter(effectAnnotations contains _.atp.safeToString).foreach { annot => 
        annot.args match {
          case List(l: Literal) =>
            import utils.RegularExpressions._

            val regex = l.value.stringValue

            RegexParser.parseString(regex) match {
              case Some(r) =>
                val nfa = RegexHelpers.regexToNFA(r);
                val dfa = nfa.determinize.minimize


                annot.atp.safeToString match {
                  case "insane.annotations.WillNotModify" =>
                    fun.contrEffects +:= AssertUntouched(r, dfa)
                  case "insane.annotations.MayOnlyModify" =>
                    fun.contrEffects +:= AssertOnlyModified(r, dfa)
                }
              case _ =>
                reporter.error("Unable to parse regex: "+regex, Some(annot.pos));
            }
          case _ =>
        }
      }
    }


    def traverseStep(tree: Tree) {
      tree match {
        case d @ DefDef(_, name, _, argsargs, _, rhs) =>
          assert(argsargs.size == 1) // We are late enough as a phase

          val (requs, enss, asss) = extractFunBody(rhs)
          val f = new NamedFunction(d.symbol, name, argsargs.head.map(_.symbol), rhs)

          extractStubInfo(f)
          extractEffectsAnotations(f)

          f.contrRequires = requs
          f.contrEnsures  = enss
          f.contrAsserts  = asss

          registerDeclaredFunction(f.symbol, f)
        case d @ Function(args, rhs) =>
          val (requs, enss, asss) = extractFunBody(rhs)
          val f = new AnnonFunction(d.symbol, args.map(_.symbol), rhs)

          extractStubInfo(f)
          extractEffectsAnotations(f)

          f.contrRequires = requs
          f.contrEnsures  = enss
          f.contrAsserts  = asss

          registerDeclaredFunction(f.symbol, f)
        case _ =>
      }
    }


    def extractFunBody(body: Tree): (Seq[Requires], Seq[Ensures], Seq[Assert]) = {
      var realBody = body
      var requs    = Seq[Requires]()
      var enss     = Seq[Ensures]()
      var asserts  = Seq[Assert]()

      realBody match {
        case ExEnsuredExpression(innerBody, resSym, contract) =>
          realBody = innerBody
          enss :+= new Ensures(contract)
        case _ =>
      }

      var continue = true
      while(continue) {
        realBody match {
          case ExRequiredExpression(innerBody, contract) =>
            realBody = innerBody
            requs :+= new Requires(contract)
          case _ =>
            continue = false
        }
      }

      def assertFind(tree: Tree) {
        tree match {
          case ExAssertEQExpression(lhs, rhs) =>
            asserts :+= new AssertEQ(tree, lhs, rhs)
          case ExAssertNEExpression(lhs, rhs) =>
            asserts :+= new AssertNE(tree, lhs, rhs)
          case _ =>
        }
      }

      new ForeachTreeTraverser(assertFind).traverse(realBody)

      (requs, enss, asserts)
    }
  }
}
