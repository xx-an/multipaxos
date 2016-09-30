# -*- coding: utf-8 -*-

import re,sys

print('=====================================================================')

def scalaEquals(classname, arglist, argString):
  eString = argString
  if classname.startswith('seta') or classname.startswith('coset'):
    eString = ' && (' + arglist[0] + '.toSet == that.' + arglist[0] + '.toSet)'
  elif classname.startswith('finfun_update_code'):
    eString = ' && (setFinFun == finfun_to_set(that, finfun_to_dom(that)) && finfun_constv(this) == finfun_constv(that))'
  elif classname.startswith('state_ext') or classname.startswith('consensus_ext'):
    eString = ''
    for i in range(0, len(arglist) - 1):
      eString += ' && this.' + arglist[i] + ' == that.' + arglist[i]
  return eString

def scalaHash(classname, arglist, hashString):
  hString = hashString
  if len(arglist) == 1:
    hString = arglist[0] + '.hashCode()'
    if classname.startswith('seta') or classname.startswith('coset'):
      hString = arglist[0] + '.toSet.hashCode()'
  else:
    if classname.startswith('finfun_update_code'):
      hString = '  41 * ( 41 + finfun_constv(this).hashCode()) + setFinFun.hashCode()'
    elif classname.startswith('state_ext') or classname.startswith('consensus_ext'):
      hString = '1'
      for i in range(0, len(arglist) - 1):
        hString = '  41 * (' + hString + ') + ' + arglist[i] + '.hashCode()'
  return hString

def scalaToString(classname, arglist):
  tString = '"' + classname + '(" + ' + arglist[0] + '.toString()'
  for i in range(1, len(arglist)):
    tString += ' + ", " + ' + arglist[i] + '.toString()'
  tString +=  ' + ")"'
  if classname.startswith('Nat'):
    tString = arglist[0] + '.toString()'
  elif classname.startswith('finfun_const'):
    tString = '"[default |-> " + ' + arglist[0] + '.toString() + "]"'
  elif classname.startswith('finfun_update_code'):
    tString = 'print_finfun_set(setFinFun) + "[default |-> " + finfun_constv(' + arglist[0] + ').toString() + "]"'
  elif classname.startswith('seta') or classname.startswith('coset'):
    tString = arglist[0] + '.mkString("{",",","}")'
  elif classname.startswith('state_ext'):
    tString = '"id: " + ' + arglist[0] + '.toString() + "    leader: " + ' + arglist[1] + '.toString() + ",    acceptors: " + ' + arglist[2] + '.toString() + ",    ballot: " + ' + arglist[3] + '.toString() + ",    firstUncommitted: " + ' + arglist[4] + '.toString() + ",    onebs: " + ' + arglist[5] + '.toString() + "\\n" + "    next_inst: " + ' + arglist[6] + '.toString() + ",    instances: " + ' + arglist[7] + '.toString() + "\\n"'
  elif classname.startswith('consensus_ext'):
    tString = '" inst: " +' + arglist[0] + '.toString() + "    view: " + ' + arglist[1] + '.toString() + ",    accepts: " + ' + arglist[2] + '.toString() + ",    status: " + ' + arglist[3] + '.toString() + ",    value:" + ' + arglist[4] + '.toString() + "\\n"'
  return tString

inputfile = sys.argv[1]
outputfile = sys.argv[2]

#Read the content of the file
scalafile = open(inputfile, 'r')
nfile = open(outputfile, 'w+')
open(outputfile, 'w').close()
nfile = open(outputfile, 'a+')

nfile.write('package lsr.paxos.core;\n')
nfile.write('\n')

line = scalafile.readline() 

while line:
  newline = line.replace("MPLib.nat","Integer").replace("MPLib.fset","List")
  if line.lstrip().startswith('final case class'):
    while re.search('extends', line) == None:
      line = scalafile.readline()
      newline += line.replace("MPLib.nat","Integer").replace("MPLib.fset","List")
    if re.search('(?<=extends)[a-zA-Z0-9\_\,\[\]]+', line.replace(" ", "")) == None:
      line = scalafile.readline()
      newline += line.replace("MPLib.nat","Integer").replace("MPLib.fset","List")

    nfile.write(newline)
    nfile.write('{\n')

    classname = re.findall('(?<=final case class\s)[a-zA-Z0-9\_\s\,\[\]]+', newline)[0]
    classname = classname.replace(" ", "")
    subclassname = re.findall('[a-zA-Z0-9\_]+', classname)[0]
    print(subclassname)

    arg = re.findall('(?<=\()[A-Za-z0-9\:\,\[\(\]\.\_\n\)]+extends',newline.replace(" ", ""))

    if arg == [')extends']:
      nfile.write('  override def equals(other: Any) = other match {\n    case that:' + classname + ' => (that.isInstanceOf[' + classname +'])'
        '\n    case _ => false\n  }\n')
      nfile.write('  override def toString = "' + classname.split('[')[0] + '"\n')
      nfile.write('  override def hashCode : Int = 41\n')
    else:
      argAll = arg[0].split(":")
    
      arglist = []
      argString = ''
      hashString = '1'

      for i in range(0, len(argAll) - 1):
        argElem = argAll[i].split(",")
        arglist.append(argElem[len(argElem) - 1].strip())
        argString += ' && this.' + arglist[i] + ' == that.' + arglist[i]
        hashString = '  41 * (' + hashString + ') + ' + arglist[i] + '.hashCode()'

      if "finfun_update_code" in classname:
        nfile.write('  val setFinFun = finfun_to_set(this, finfun_to_dom(this))\n')
      nfile.write('  override def equals(other: Any) = other match {\n    case that:' + classname + ' => (that.isInstanceOf[' + classname +'])' + scalaEquals(subclassname, arglist, argString)
        + '\n    case _ => false\n  }\n')
      nfile.write('  override def toString = ' + scalaToString(subclassname, arglist) + '\n')
      nfile.write('  override def hashCode : Int = ' + scalaHash(subclassname, arglist, hashString) + '\n')
    nfile.write('}\n')
    if "finfun_update_code" in classname:
      nfile.write('\ndef finfun_to_dom[A, B](x0: finfun[A, B]): Set[A] = x0 match {\n  case finfun_update_code(f, a, b) => (\n    if (eq[B](b, finfun_constv[A, B](f)))\n      finfun_to_dom[A, B](f) - a\n    else\n      finfun_to_dom[A, B](f) + a)\n  case finfun_const(c) => Set()\n}\n\ndef finfun_to_set[A, B](x0: finfun[A, B], domA: Set[A]): Set[(A,B)] = {\n  var setFinFun: Set[(A,B)] = Set()\n  domA.foreach { a => setFinFun += Tuple2(a, finfun_apply(x0, a)) }\n  setFinFun\n}\n\ndef print_finfun_set[A,B](setfinfun: Set[(A,B)]): String = {\n  var strfinfun : String = ""\n  setfinfun.foreach { case (a,b) => (\n      strfinfun += "[" + a.toString() + " |-> " + b.toString() + "]"\n    )}\n  strfinfun\n}\n\ndef finfun_constv[A, B](x0: finfun[A, B]): B = x0 match {\n  case finfun_update_code(f, a, b) => finfun_constv[A, B](f)\n  case finfun_const(c) => c\n}\n')
  elif "implicit def equal_list" in line:
    nfile.write("implicit def equal_t[A] : equal[A] = new equal[A] {\n  val `MPLib.equal` = (a : A, b: A) => a == b\n}\n\n")
    nfile.write("implicit def ord_int: ord[Integer] = new ord[Integer] {\n  val `MPLib.less_eq` = (a: Integer, b: Integer) => a <= b\n  val `MPLib.less` = (a: Integer, b: Integer) => a < b\n}\n\n")
    nfile.write("def finite_UNIV_inta: phantom[Integer, Boolean] = phantoma[Boolean, Integer](false)\n\n")
    nfile.write("implicit def finite_UNIV_int: finite_UNIV[Integer] = new finite_UNIV[Integer] {\n  val `MPLib.finite_UNIV` = finite_UNIV_inta\n}\n\n")
    nfile.write("def card_UNIV_inta: phantom[Integer, nat] = phantoma[nat, Integer](zero_nat)\n\n")
    nfile.write("implicit def card_UNIV_int: card_UNIV[Integer] = new card_UNIV[Integer] {\n  val `MPLib.card_UNIV` = card_UNIV_inta\n  val `MPLib.finite_UNIV` = finite_UNIV_inta\n}\n\n")
    nfile.write("implicit def linorder_int: linorder[Integer] = new linorder[Integer] {\n  val `MPLib.less_eq` = (a: Integer, b: Integer) => a <= b\n  val `MPLib.less` = (a: Integer, b: Integer) => a < b\n}\n\n")
    nfile.write(line)
  elif "def equal_consensus_exta" in line:
    nfile.write("def equal_int(a:Integer, b:Integer) : Boolean = (a == b)\n")
    nfile.write("def times_int(a:Integer, b:Integer) : Integer = (a * b)\n")
    nfile.write("def less_int(a:Integer, b:Integer) : Boolean = a < b\n")
    nfile.write("def less_eq_int(a:Integer, b:Integer) : Boolean = a <= b\n")
    nfile.write("def minus_int(a:Integer, b:Integer) : Integer = a - b\n")
    nfile.write("def plus_int(a:Integer, b:Integer) : Integer = a + b\n")
    nfile.write("def mod_int(a:Integer, b:Integer) : Integer = a % b\n")
    nfile.write("def divide_int(a:Integer, b:Integer) : Integer = (a / b)\n")
    nfile.write("def suc_int(a:Integer) : Integer = (a + 1)\n")
    nfile.write("def upt_int(i: Integer, j: Integer): List[Integer] =\n  (if (i < j) i :: upt_int(i + 1, j) else Nil)\n\n")
    nfile.write("def remove_list[A](x: A, xa1: List[A]): List[A] = (x, xa1) match {\n  case (x, Nil) => Nil\n  case (x, y :: xs) => (if (x == y) remove_list[A](x, xs) else y :: remove_list[A](x, xs))\n}\n")
    nfile.write("def minus_list[A](a: List[A], x1: List[A]): List[A] = {\n    MPLib.fold[A, List[A]](((aa: A) => (b: List[A]) => remove_list[A](aa, b)), x1, a)\n}\n")
    nfile.write("def member_list[A](x0: List[A], y: A): Boolean = (x0, y) match {\n  case (Nil, y) => false\n  case (x :: xs, y) => (x == y) || member_list[A](xs, y)\n}\n")
    nfile.write("def insert_list[A](x: A, xs: List[A]): List[A] = {\n  (if (member_list[A](xs, x)) xs else x :: xs)\n}\n")
    nfile.write("def sup_list[A](x0: List[A], a: List[A]): List[A] = {\n    MPLib.fold[A, List[A]](((aa: A) => (b: List[A]) => insert_list[A](aa, b)), x0, a)\n}\n\n")
    nfile.write("def list_nth[A](x0: List[A], a: Integer): A = {x0.apply(a)}\n\n")
    nfile.write("def getCmdVal[A](a : cmd[A]) : A = a match\n{\n  case(Comd(x1)) => x1\n}\n\n")
    nfile.write("def getFwdFields[A](message : msg[A]) : A = message match {\n  case(Fwd(a)) => a\n}\n\n")
    nfile.write("def getFields[A](pack : packet[A]) : (Integer, Integer, msg[A]) = pack match {\n  case(Packet(a,b,c)) => (a,b,c)\n}\n")
    nfile.write('\n')
    nfile.write(line)
  elif line.startswith("object MultiPaxos"):
    nfile.write("object MultiPaxosImpl {\n")
  else:
    line1=line.replace("MPLib.nat","Integer").replace("MPLib.zero_nat","0").replace("MPLib.one_nat","1").replace("MPLib.equal_nata","equal_int").replace("MPLib.fset","List")
    line2=line1.replace("MPLib.minus_nat","minus_int").replace("MPLib.plus_nat","plus_int").replace("MPLib.set","List").replace("MPLib.less_nat","less_int").replace("MPLib.less_eq_nat","less_eq_int")
    line3=line2.replace("Integer_of_integer(BigInt","(").replace("MPLib.times_nat","times_int").replace("MPLib.divide_nat","divide_int").replace("MPLib.mod_nat","mod_int")
    line4=line3.replace("MPLib.Suc","suc_int").replace("MPLib.upt","upt_int").replace("MPLib.bot_set[Integer]","List()").replace("MPLib.bot_set[packet[A]]","List()").replace("MPLib.minus_set","minus_list")
    line5=line4.replace("MPLib.fimage","MPLib.map").replace("MPLib.sup_set","sup_list").replace("MPLib.nth","list_nth")
    nfile.write(line5)
  line = scalafile.readline()

scalafile.close()
nfile.close()

