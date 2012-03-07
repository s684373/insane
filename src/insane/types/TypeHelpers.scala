package insane
package types

import utils.Reporters._

trait TypeHelpers { self: AnalysisComponent =>

  import global._

  def isGroundClass(s: Symbol) = atPhase(currentRun.typerPhase){s.tpe.parents exists (s => s.typeSymbol == definitions.AnyValClass)}

  def isGroundOSET(oset: ObjectSet) = (oset.exactTypes.size == 1) && isGroundClass(oset.exactTypes.head.typeSymbol) && oset.exactTypes.head != definitions.BooleanClass.tpe

  def instantiateChildTypeParameters(parentTpe: Type, childTpe: Type): Option[(Type, Map[Symbol, Type])] = {
    val childSym  = childTpe.typeSymbol
    val parentSym = parentTpe.typeSymbol

    if (childTpe == parentTpe) {
      return Some((childTpe, childSym.typeParams.map(s => (s, s.tpe)).toMap))
    }

    val parentAppliedType = parentTpe
    val childTypeVars     = childSym.typeParams.map(s => TypeVar(s.tpeHK, new TypeConstraint, Nil, Nil))
    
    val childAppliedType  = appliedType(childSym.tpe, childTypeVars)

    println("childSym            = "+childSym)
    println("parentSym           = "+parentSym)
    println("parentAppliedType   = "+parentAppliedType)
    println("childTypeVars       = "+childTypeVars)
    println("childAppliedType    = "+childAppliedType)

    val skolems = new scala.collection.mutable.ListBuffer[TypeSymbol]

    object tvToSkolem extends VariantTypeMap {
      def apply(tp: Type) = mapOver(tp) match {
        case tv: TypeVar =>
          val tpSym  = tv.origin.typeSymbol
          val bounds = TypeBounds(glb(tv.constr.loBounds), lub(tv.constr.hiBounds))
          val skolem = tpSym.owner.newExistentialSkolem(tpSym, tpSym) setInfo bounds
          skolems += skolem
          skolem.tpe
        case tp1 => tp1
      }
    }

    if (childAppliedType <:< parentAppliedType) {
      val tp   = tvToSkolem(childAppliedType)
      Some((newExistentialType(skolems.toList, tp), (childSym.typeParams zip skolems.map(_.tpe).toList).toMap))
    } else {
      None
    }
  }

  def getMatchingMethods(methodName: Name, methodType: Type, oset: ObjectSet, pos: Position, silent: Boolean): Set[Symbol] = {

    var failures = Set[Type]();

    def getMatchingMethodIn(parentTpe: Type, childTpe: Type): Option[Symbol] = {
      var res: Option[Symbol] = None

      println(" ==> Matching "+childTpe+" <: "+parentTpe+" for method "+methodType)

      instantiateChildTypeParameters(parentTpe, childTpe) match {
        case Some((actualChildTpe, map)) =>
          println(" ~~> "+actualChildTpe+" with map "+map)

          var tpeChain = if (parentTpe == childTpe) {
            actualChildTpe.baseTypeSeq.toList
          } else {
            List(actualChildTpe)
          }

          for (tpe <- tpeChain if res.isEmpty) {
            val found = tpe.decls.lookupAll(methodName).find{sym => 
              println("In "+tpe+" ("+sym.tpe+"):") 

              val foundType  = tpe.memberType(sym)
              val targetType = methodType.asSeenFrom(tpe, tpe.typeSymbol)

              println(" is "+foundType+" <:< "+targetType+"?") 
              println(if (foundType <:< targetType) "yes" else "no")

              foundType <:< targetType
            }

            if (!found.isEmpty) {
              res = Some(found.get)
            }
          }

          if (res.isEmpty && actualChildTpe.typeSymbol != definitions.NothingClass) {
            failures += childTpe
          }
        case None =>
          println(" ~~> <!> impossible ")
          // Incompatible
      }


      res match {
        // We ignore abstract methods
        case Some(m) if m.isDeferred =>
          None
        case r =>
          r
      }
    }

    val typeTuples =
      (oset.exactTypes).map(t => (t, t)) ++
      (oset.subtypesOf).flatMap(st => getDescendents(st.typeSymbol).map(s => (st, s.tpe)))

    println(typeTuples)

    val r = typeTuples map { case (t, ct) => getMatchingMethodIn(t, ct) } collect { case Some(ms) => ms }

    def conciseSet(a: Traversable[_]) = if (a.size > 5) {
      (a.take(5) ++ List(" "+(a.size-5)+" more...")).mkString("{", ",", "}");
    } else {
      a.mkString("{", ",", "}");
    }

    if (!failures.isEmpty && !silent) {
      reporter.warn("Failed to find method "+methodName+": "+methodType+" in classes "+conciseSet(failures)+" amongst "+conciseSet(oset.exactTypes), pos)
    }

    r
  }

  def arrayType(tpe: Type) =
    TypeRef(NoPrefix, definitions.ArrayClass, List(tpe))

  def methodReturnType(methodSymbol: Symbol): ObjectSet = {
    val resType = methodSymbol.tpe.resultType

    val r = resType match {
      case TypeRef(_, definitions.ArrayClass, List(tpe)) =>
        // resType is a parametrized array, we keep that type precise, ignore
        // descendents in this case
        ObjectSet.singleton(resType)
      case _ =>
        // General case
        ObjectSet.subtypesOf(resType)
    }
    r
  }

  def computeTypeMap(meth: Symbol, callTypeParams: Seq[Tree], receiverTypes: ObjectSet): TypeMap = {
    val methTypeMap: Map[Symbol, Type] = meth.tpe match {
      case PolyType(params, _) =>
        (params zip callTypeParams).map{ case (a,v) => a -> v.tpe }.toMap
      case t =>
        Map()
    }

    val classTypeMap: Map[Symbol, Set[Type]] = meth.owner.tpe.typeArgs.map{ t =>
      t.typeSymbol -> receiverTypes.exactTypes.map{tt => 
        t.asSeenFrom(tt, meth.owner)}.toSet
    }.toMap

    TypeMap(classTypeMap, methTypeMap)
  }

  class SubstSkolemsTypeMap(from: List[Symbol], to: List[Type]) extends SubstTypeMap(from, to) {
    protected override def matches(sym1: Symbol, sym2: Symbol) =
      if (sym2.isTypeSkolem) sym2.deSkolemize eq sym1
      else sym1 eq sym2
  }

  case class TypeMap(classTypeMap: Map[Symbol, Set[Type]], methodTypeMap: Map[Symbol, Type]) {
    val methodTypeMapSingle = methodTypeMap.toList.unzip
    val classTypeMapSingle  = classTypeMap.map{ case (s, tpes) => (s, lub(tpes.toList)) }.toList.unzip

    val mapSkolems = new SubstSkolemsTypeMap(methodTypeMapSingle._1, methodTypeMapSingle._2)

    def apply(t: Type): Type = {
      mapSkolems(t.instantiateTypeParams(classTypeMapSingle._1, classTypeMapSingle._2))
    }

    def apply(oset: ObjectSet): ObjectSet = {
      var newOset = if (methodTypeMap.isEmpty) {
        oset
      } else {
        ObjectSet(oset.subtypesOf.map(mapSkolems), oset.exactTypes.map(mapSkolems))
      }

      for ((from, tos) <- classTypeMap) {
        var subst = Set[Type]()
        var exact = Set[Type]()
        for (to <- tos) {
          subst ++= newOset.subtypesOf.map(_.instantiateTypeParams(List(from), List(to)))
          exact ++= newOset.exactTypes.map(_.instantiateTypeParams(List(from), List(to)))
        }
        newOset = ObjectSet(subst, exact)
      }

      newOset
    }

    val isEmpty = classTypeMap.isEmpty && methodTypeMap.isEmpty

    override def toString = "{C: "+classTypeMap.mkString(", ")+" | M: "+methodTypeMap.mkString(", ")+"}"
  }
                      
}
