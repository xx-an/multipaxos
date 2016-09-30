package lsr.paxos.core;

object MPLib {

abstract sealed class nat
final case class Nat(a: BigInt) extends nat
{
  override def equals(other: Any) = other match {
    case that:Nat => (that.isInstanceOf[Nat]) && this.a == that.a
    case _ => false
  }
  override def toString = a.toString()
  override def hashCode : Int = a.hashCode()
}

def integer_of_nat(x0: nat): BigInt = x0 match {
  case Nat(x) => x
}

def equal_nata(m: nat, n: nat): Boolean = integer_of_nat(m) == integer_of_nat(n)

trait equal[A] {
  val `MPLib.equal`: (A, A) => Boolean
}
def equal[A](a: A, b: A)(implicit A: equal[A]): Boolean = A.`MPLib.equal`(a, b)

implicit def equal_nat: equal[nat] = new equal[nat] {
  val `MPLib.equal` = (a: nat, b: nat) => equal_nata(a, b)
}

def less_eq_nat(m: nat, n: nat): Boolean =
  integer_of_nat(m) <= integer_of_nat(n)

trait ord[A] {
  val `MPLib.less_eq`: (A, A) => Boolean
  val `MPLib.less`: (A, A) => Boolean
}
def less_eq[A](a: A, b: A)(implicit A: ord[A]): Boolean =
  A.`MPLib.less_eq`(a, b)
def less[A](a: A, b: A)(implicit A: ord[A]): Boolean = A.`MPLib.less`(a, b)

def less_nat(m: nat, n: nat): Boolean = integer_of_nat(m) < integer_of_nat(n)

implicit def ord_nat: ord[nat] = new ord[nat] {
  val `MPLib.less_eq` = (a: nat, b: nat) => less_eq_nat(a, b)
  val `MPLib.less` = (a: nat, b: nat) => less_nat(a, b)
}

trait preorder[A] extends ord[A] {
}

trait order[A] extends preorder[A] {
}

implicit def preorder_nat: preorder[nat] = new preorder[nat] {
  val `MPLib.less_eq` = (a: nat, b: nat) => less_eq_nat(a, b)
  val `MPLib.less` = (a: nat, b: nat) => less_nat(a, b)
}

implicit def order_nat: order[nat] = new order[nat] {
  val `MPLib.less_eq` = (a: nat, b: nat) => less_eq_nat(a, b)
  val `MPLib.less` = (a: nat, b: nat) => less_nat(a, b)
}

trait linorder[A] extends order[A] {
}

implicit def linorder_nat: linorder[nat] = new linorder[nat] {
  val `MPLib.less_eq` = (a: nat, b: nat) => less_eq_nat(a, b)
  val `MPLib.less` = (a: nat, b: nat) => less_nat(a, b)
}

abstract sealed class phantom[A, B]
final case class phantoma[B, A](a: B) extends phantom[A, B]
{
  override def equals(other: Any) = other match {
    case that:phantoma[B,A] => (that.isInstanceOf[phantoma[B,A]]) && this.a == that.a
    case _ => false
  }
  override def toString = "phantoma(" + a.toString() + ")"
  override def hashCode : Int = a.hashCode()
}

def finite_UNIV_nata: phantom[nat, Boolean] = phantoma[Boolean, nat](false)

def zero_nat: nat = Nat(BigInt(0))

def card_UNIV_nata: phantom[nat, nat] = phantoma[nat, nat](zero_nat)

trait finite_UNIV[A] {
  val `MPLib.finite_UNIV`: phantom[A, Boolean]
}
def finite_UNIV[A](implicit A: finite_UNIV[A]): phantom[A, Boolean] =
  A.`MPLib.finite_UNIV`

trait card_UNIV[A] extends finite_UNIV[A] {
  val `MPLib.card_UNIV`: phantom[A, nat]
}
def card_UNIV[A](implicit A: card_UNIV[A]): phantom[A, nat] =
  A.`MPLib.card_UNIV`

implicit def finite_UNIV_nat: finite_UNIV[nat] = new finite_UNIV[nat] {
  val `MPLib.finite_UNIV` = finite_UNIV_nata
}

implicit def card_UNIV_nat: card_UNIV[nat] = new card_UNIV[nat] {
  val `MPLib.card_UNIV` = card_UNIV_nata
  val `MPLib.finite_UNIV` = finite_UNIV_nata
}

def eq[A : equal](a: A, b: A): Boolean = equal[A](a, b)

def equal_lista[A : equal](x0: List[A], x1: List[A]): Boolean = (x0, x1) match {
  case (Nil, x21 :: x22) => false
  case (x21 :: x22, Nil) => false
  case (x21 :: x22, y21 :: y22) => eq[A](x21, y21) && equal_lista[A](x22, y22)
  case (Nil, Nil) => true
}

implicit def equal_t[A] : equal[A] = new equal[A] {
  val `MPLib.equal` = (a : A, b: A) => a == b
}

implicit def ord_int: ord[Integer] = new ord[Integer] {
  val `MPLib.less_eq` = (a: Integer, b: Integer) => a <= b
  val `MPLib.less` = (a: Integer, b: Integer) => a < b
}

def finite_UNIV_inta: phantom[Integer, Boolean] = phantoma[Boolean, Integer](false)

implicit def finite_UNIV_int: finite_UNIV[Integer] = new finite_UNIV[Integer] {
  val `MPLib.finite_UNIV` = finite_UNIV_inta
}

def card_UNIV_inta: phantom[Integer, nat] = phantoma[nat, Integer](zero_nat)

implicit def card_UNIV_int: card_UNIV[Integer] = new card_UNIV[Integer] {
  val `MPLib.card_UNIV` = card_UNIV_inta
  val `MPLib.finite_UNIV` = finite_UNIV_inta
}

implicit def linorder_int: linorder[Integer] = new linorder[Integer] {
  val `MPLib.less_eq` = (a: Integer, b: Integer) => a <= b
  val `MPLib.less` = (a: Integer, b: Integer) => a < b
}

implicit def equal_list[A : equal]: equal[List[A]] = new equal[List[A]] {
  val `MPLib.equal` = (a: List[A], b: List[A]) => equal_lista[A](a, b)
}

def equal_proda[A : equal, B : equal](x0: (A, B), x1: (A, B)): Boolean =
  (x0, x1) match {
  case ((x1, x2), (y1, y2)) => eq[A](x1, y1) && eq[B](x2, y2)
}

implicit def equal_prod[A : equal, B : equal]: equal[(A, B)] = new equal[(A, B)]
  {
  val `MPLib.equal` = (a: (A, B), b: (A, B)) => equal_proda[A, B](a, b)
}

def equal_unita(u: Unit, v: Unit): Boolean = true

implicit def equal_unit: equal[Unit] = new equal[Unit] {
  val `MPLib.equal` = (a: Unit, b: Unit) => equal_unita(a, b)
}

implicit def ord_integer: ord[BigInt] = new ord[BigInt] {
  val `MPLib.less_eq` = (a: BigInt, b: BigInt) => a <= b
  val `MPLib.less` = (a: BigInt, b: BigInt) => a < b
}

abstract sealed class num
final case class One() extends num
{
  override def equals(other: Any) = other match {
    case that:One => (that.isInstanceOf[One])
    case _ => false
  }
  override def toString = "One"
  override def hashCode : Int = 41
}
final case class Bit0(a: num) extends num
{
  override def equals(other: Any) = other match {
    case that:Bit0 => (that.isInstanceOf[Bit0]) && this.a == that.a
    case _ => false
  }
  override def toString = "Bit0(" + a.toString() + ")"
  override def hashCode : Int = a.hashCode()
}
final case class Bit1(a: num) extends num
{
  override def equals(other: Any) = other match {
    case that:Bit1 => (that.isInstanceOf[Bit1]) && this.a == that.a
    case _ => false
  }
  override def toString = "Bit1(" + a.toString() + ")"
  override def hashCode : Int = a.hashCode()
}

abstract sealed class set[A]
final case class seta[A](a: List[A]) extends set[A]
{
  override def equals(other: Any) = other match {
    case that:seta[A] => (that.isInstanceOf[seta[A]]) && (a.toSet == that.a.toSet)
    case _ => false
  }
  override def toString = a.mkString("{",",","}")
  override def hashCode : Int = a.toSet.hashCode()
}
final case class coset[A](a: List[A]) extends set[A]
{
  override def equals(other: Any) = other match {
    case that:coset[A] => (that.isInstanceOf[coset[A]]) && (a.toSet == that.a.toSet)
    case _ => false
  }
  override def toString = a.mkString("{",",","}")
  override def hashCode : Int = a.toSet.hashCode()
}

abstract sealed class nibble
final case class Nibble0() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble0 => (that.isInstanceOf[Nibble0])
    case _ => false
  }
  override def toString = "Nibble0"
  override def hashCode : Int = 41
}
final case class Nibble1() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble1 => (that.isInstanceOf[Nibble1])
    case _ => false
  }
  override def toString = "Nibble1"
  override def hashCode : Int = 41
}
final case class Nibble2() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble2 => (that.isInstanceOf[Nibble2])
    case _ => false
  }
  override def toString = "Nibble2"
  override def hashCode : Int = 41
}
final case class Nibble3() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble3 => (that.isInstanceOf[Nibble3])
    case _ => false
  }
  override def toString = "Nibble3"
  override def hashCode : Int = 41
}
final case class Nibble4() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble4 => (that.isInstanceOf[Nibble4])
    case _ => false
  }
  override def toString = "Nibble4"
  override def hashCode : Int = 41
}
final case class Nibble5() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble5 => (that.isInstanceOf[Nibble5])
    case _ => false
  }
  override def toString = "Nibble5"
  override def hashCode : Int = 41
}
final case class Nibble6() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble6 => (that.isInstanceOf[Nibble6])
    case _ => false
  }
  override def toString = "Nibble6"
  override def hashCode : Int = 41
}
final case class Nibble7() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble7 => (that.isInstanceOf[Nibble7])
    case _ => false
  }
  override def toString = "Nibble7"
  override def hashCode : Int = 41
}
final case class Nibble8() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble8 => (that.isInstanceOf[Nibble8])
    case _ => false
  }
  override def toString = "Nibble8"
  override def hashCode : Int = 41
}
final case class Nibble9() extends nibble
{
  override def equals(other: Any) = other match {
    case that:Nibble9 => (that.isInstanceOf[Nibble9])
    case _ => false
  }
  override def toString = "Nibble9"
  override def hashCode : Int = 41
}
final case class NibbleA() extends nibble
{
  override def equals(other: Any) = other match {
    case that:NibbleA => (that.isInstanceOf[NibbleA])
    case _ => false
  }
  override def toString = "NibbleA"
  override def hashCode : Int = 41
}
final case class NibbleB() extends nibble
{
  override def equals(other: Any) = other match {
    case that:NibbleB => (that.isInstanceOf[NibbleB])
    case _ => false
  }
  override def toString = "NibbleB"
  override def hashCode : Int = 41
}
final case class NibbleC() extends nibble
{
  override def equals(other: Any) = other match {
    case that:NibbleC => (that.isInstanceOf[NibbleC])
    case _ => false
  }
  override def toString = "NibbleC"
  override def hashCode : Int = 41
}
final case class NibbleD() extends nibble
{
  override def equals(other: Any) = other match {
    case that:NibbleD => (that.isInstanceOf[NibbleD])
    case _ => false
  }
  override def toString = "NibbleD"
  override def hashCode : Int = 41
}
final case class NibbleE() extends nibble
{
  override def equals(other: Any) = other match {
    case that:NibbleE => (that.isInstanceOf[NibbleE])
    case _ => false
  }
  override def toString = "NibbleE"
  override def hashCode : Int = 41
}
final case class NibbleF() extends nibble
{
  override def equals(other: Any) = other match {
    case that:NibbleF => (that.isInstanceOf[NibbleF])
    case _ => false
  }
  override def toString = "NibbleF"
  override def hashCode : Int = 41
}

abstract sealed class char
final case class Char(a: nibble, b: nibble) extends char
{
  override def equals(other: Any) = other match {
    case that:Char => (that.isInstanceOf[Char]) && this.a == that.a && this.b == that.b
    case _ => false
  }
  override def toString = "Char(" + a.toString() + ", " + b.toString() + ")"
  override def hashCode : Int =   41 * (  41 * (1) + a.hashCode()) + b.hashCode()
}

abstract sealed class finfun[A, B]
final case class finfun_const[B, A](a: B) extends finfun[A, B]
{
  override def equals(other: Any) = other match {
    case that:finfun_const[B,A] => (that.isInstanceOf[finfun_const[B,A]]) && this.a == that.a
    case _ => false
  }
  override def toString = "[default |-> " + a.toString() + "]"
  override def hashCode : Int = a.hashCode()
}
final case class finfun_update_code[A, B](a: finfun[A, B], b: A, c: B) extends
  finfun[A, B]
{
  val setFinFun = finfun_to_set(this, finfun_to_dom(this))
  override def equals(other: Any) = other match {
    case that:finfun_update_code[A,B] => (that.isInstanceOf[finfun_update_code[A,B]]) && (setFinFun == finfun_to_set(that, finfun_to_dom(that)) && finfun_constv(this) == finfun_constv(that))
    case _ => false
  }
  override def toString = print_finfun_set(setFinFun) + "[default |-> " + finfun_constv(a).toString() + "]"
  override def hashCode : Int =   41 * ( 41 + finfun_constv(this).hashCode()) + setFinFun.hashCode()
}

def finfun_to_dom[A, B](x0: finfun[A, B]): Set[A] = x0 match {
  case finfun_update_code(f, a, b) => (
    if (eq[B](b, finfun_constv[A, B](f)))
      finfun_to_dom[A, B](f) - a
    else
      finfun_to_dom[A, B](f) + a)
  case finfun_const(c) => Set()
}

def finfun_to_set[A, B](x0: finfun[A, B], domA: Set[A]): Set[(A,B)] = {
  var setFinFun: Set[(A,B)] = Set()
  domA.foreach { a => setFinFun += Tuple2(a, finfun_apply(x0, a)) }
  setFinFun
}

def print_finfun_set[A,B](setfinfun: Set[(A,B)]): String = {
  var strfinfun : String = ""
  setfinfun.foreach { case (a,b) => (
      strfinfun += "[" + a.toString() + " |-> " + b.toString() + "]"
    )}
  strfinfun
}

def finfun_constv[A, B](x0: finfun[A, B]): B = x0 match {
  case finfun_update_code(f, a, b) => finfun_constv[A, B](f)
  case finfun_const(c) => c
}

def id[A]: A => A = ((x: A) => x)

def plus_nat(m: nat, n: nat): nat = Nat(integer_of_nat(m) + integer_of_nat(n))

def one_nat: nat = Nat(BigInt(1))

def Suc(n: nat): nat = plus_nat(n, one_nat)

def comp[A, B, C](f: A => B, g: C => A): C => B = ((x: C) => f(g(x)))

def max[A : ord](a: A, b: A): A = (if (less_eq[A](a, b)) b else a)

def minus_nat(m: nat, n: nat): nat =
  Nat(max[BigInt](BigInt(0), integer_of_nat(m) - integer_of_nat(n)))

def nth[A](x0: List[A], n: nat): A = (x0, n) match {
  case (x :: xs, n) =>
    (if (equal_nata(n, zero_nat)) x else nth[A](xs, minus_nat(n, one_nat)))
}

def fold[A, B](f: A => B => B, x1: List[A], s: B): B = (f, x1, s) match {
  case (f, x :: xs, s) => fold[A, B](f, xs, (f(x))(s))
  case (f, Nil, s) => s
}

def rev[A](xs: List[A]): List[A] =
  fold[A, List[A]](((a: A) => (b: List[A]) => a :: b), xs, Nil)

def upt(i: nat, j: nat): List[nat] =
  (if (less_nat(i, j)) i :: upt(Suc(i), j) else Nil)

def nulla[A](x0: List[A]): Boolean = x0 match {
  case Nil => true
  case x :: xs => false
}

def map[A, B](f: A => B, x1: List[A]): List[B] = (f, x1) match {
  case (f, Nil) => Nil
  case (f, x21 :: x22) => f(x21) :: map[A, B](f, x22)
}

def image[A, B](f: A => B, x1: set[A]): set[B] = (f, x1) match {
  case (f, seta(xs)) => seta[B](map[A, B](f, xs))
}

def foldr[A, B](f: A => B => B, x1: List[A]): B => B = (f, x1) match {
  case (f, Nil) => id[B]
  case (f, x :: xs) => comp[B, B, B](f(x), foldr[A, B](f, xs))
}

def membera[A : equal](x0: List[A], y: A): Boolean = (x0, y) match {
  case (Nil, y) => false
  case (x :: xs, y) => eq[A](x, y) || membera[A](xs, y)
}

def insert[A : equal](x: A, xs: List[A]): List[A] =
  (if (membera[A](xs, x)) xs else x :: xs)

def union[A : equal]: (List[A]) => (List[A]) => List[A] =
  ((a: List[A]) => (b: List[A]) =>
    fold[A, List[A]](((aa: A) => (ba: List[A]) => insert[A](aa, ba)), a, b))

def member[A : equal](x: A, xa1: set[A]): Boolean = (x, xa1) match {
  case (x, coset(xs)) => ! (membera[A](xs, x))
  case (x, seta(xs)) => membera[A](xs, x)
}

def filter[A](p: A => Boolean, x1: List[A]): List[A] = (p, x1) match {
  case (p, Nil) => Nil
  case (p, x :: xs) => (if (p(x)) x :: filter[A](p, xs) else filter[A](p, xs))
}

def hd[A](x0: List[A]): A = x0 match {
  case x21 :: x22 => x21
}

def remdups[A : equal](x0: List[A]): List[A] = x0 match {
  case Nil => Nil
  case x :: xs =>
    (if (membera[A](xs, x)) remdups[A](xs) else x :: remdups[A](xs))
}

def remove1[A : equal](x: A, xa1: List[A]): List[A] = (x, xa1) match {
  case (x, Nil) => Nil
  case (x, y :: xs) => (if (eq[A](x, y)) xs else y :: remove1[A](x, xs))
}

def gen_length[A](n: nat, x1: List[A]): nat = (n, x1) match {
  case (n, x :: xs) => gen_length[A](Suc(n), xs)
  case (n, Nil) => n
}

def of_phantom[A, B](x0: phantom[A, B]): B = x0 match {
  case phantoma(x) => x
}

def size_list[A]: (List[A]) => nat =
  ((a: List[A]) => gen_length[A](zero_nat, a))

def card[A : card_UNIV : equal](x0: set[A]): nat = x0 match {
  case coset(xs) =>
    minus_nat(of_phantom[A, nat](card_UNIV[A]),
               size_list[A].apply(remdups[A](xs)))
  case seta(xs) => size_list[A].apply(remdups[A](xs))
}

def finfun_update[A : equal, B : equal](x0: finfun[A, B], a: A, b: B):
      finfun[A, B]
  =
  (x0, a, b) match {
  case (finfun_update_code(f, aa, ba), a, b) =>
    (if (eq[A](aa, a)) finfun_update[A, B](f, aa, b)
      else finfun_update_code[A, B](finfun_update[A, B](f, a, b), aa, ba))
  case (finfun_const(ba), a, b) =>
    (if (eq[B](ba, b)) finfun_const[B, A](ba)
      else finfun_update_code[A, B](finfun_const[B, A](ba), a, b))
}

def finfun_apply[A : equal, B](x0: finfun[A, B], a: A): B = (x0, a) match {
  case (finfun_const(b), a) => b
  case (finfun_update_code(f, aa, b), a) =>
    (if (eq[A](aa, a)) b else finfun_apply[A, B](f, a))
}

def finfun_Diag[A : equal, B : equal,
                 C : equal](x0: finfun[A, B], g: finfun[A, C]):
      finfun[A, (B, C)]
  =
  (x0, g) match {
  case (finfun_update_code(f, a, b), g) =>
    finfun_update[A, (B, C)](finfun_Diag[A, B, C](f, g), a,
                              (b, finfun_apply[A, C](g, a)))
  case (finfun_const(b), finfun_update_code(g, a, c)) =>
    finfun_update_code[A, (B, C)](finfun_Diag[A, B,
       C](finfun_const[B, A](b), g),
                                   a, (b, c))
  case (finfun_const(b), finfun_const(c)) => finfun_const[(B, C), A]((b, c))
}

def finfun_comp[A, B, C](g: A => B, x1: finfun[C, A]): finfun[C, B] = (g, x1)
  match {
  case (g, finfun_update_code(f, a, b)) =>
    finfun_update_code[C, B](finfun_comp[A, B, C](g, f), a, g(b))
  case (g, finfun_const(c)) => finfun_const[B, C](g(c))
}

def apsnd[A, B, C](f: A => B, x1: (C, A)): (C, B) = (f, x1) match {
  case (f, (x, y)) => (x, f(y))
}

def top_set[A]: set[A] = coset[A](Nil)

def finfun_default[A : card_UNIV : equal, B](x0: finfun[A, B]): B = x0 match {
  case finfun_update_code(f, a, b) => finfun_default[A, B](f)
  case finfun_const(c) =>
    (if (equal_nata(card[A](top_set[A]), zero_nat)) c
      else sys.error("undefined"))
}

def insort_key[A, B : linorder](f: A => B, x: A, xa2: List[A]): List[A] =
  (f, x, xa2) match {
  case (f, x, Nil) => List(x)
  case (f, x, y :: ys) =>
    (if (less_eq[B](f(x), f(y))) x :: y :: ys
      else y :: insort_key[A, B](f, x, ys))
}

def insort_insert_key[A, B : equal : linorder](f: A => B, x: A, xs: List[A]):
      List[A]
  =
  (if (member[B](f(x), image[A, B](f, seta[A](xs)))) xs
    else insort_key[A, B](f, x, xs))

def finfun_to_list[A : card_UNIV : equal : linorder,
                    B : equal](x0: finfun[A, B]):
      List[A]
  =
  x0 match {
  case finfun_update_code(f, a, b) =>
    (if (eq[B](b, finfun_default[A, B](f)))
      remove1[A](a, finfun_to_list[A, B](f))
      else insort_insert_key[A, A](((x: A) => x), a, finfun_to_list[A, B](f)))
  case finfun_const(c) =>
    (if (equal_nata(card[A](top_set[A]), zero_nat)) Nil
      else { sys.error("finfun_to_list called on finite type");
             (((_: Unit) =>
                finfun_to_list[A, B](finfun_const[B, A](c)))).apply(())
             })
}

def fst[A, B](x0: (A, B)): A = x0 match {
  case (x1, x2) => x1
}

def snd[A, B](x0: (A, B)): B = x0 match {
  case (x1, x2) => x2
}

def sgn_integer(k: BigInt): BigInt =
  (if (k == BigInt(0)) BigInt(0)
    else (if (k < BigInt(0)) BigInt(-1) else BigInt(1)))

def divmod_integer(k: BigInt, l: BigInt): (BigInt, BigInt) =
  (if (k == BigInt(0)) (BigInt(0), BigInt(0))
    else (if (l == BigInt(0)) (BigInt(0), k)
           else (comp[BigInt, ((BigInt, BigInt)) => (BigInt, BigInt),
                       BigInt](comp[BigInt => BigInt,
                                     ((BigInt, BigInt)) => (BigInt, BigInt),
                                     BigInt](((a: BigInt => BigInt) =>
       (b: (BigInt, BigInt)) => apsnd[BigInt, BigInt, BigInt](a, b)),
      ((a: BigInt) => (b: BigInt) => a * b)),
                                ((a: BigInt) =>
                                  sgn_integer(a)))).apply(l).apply((if (sgn_integer(k) ==
                                  sgn_integer(l))
                             ((k: BigInt) => (l: BigInt) => if (l == 0)
                               (BigInt(0), k) else
                               (k.abs /% l.abs)).apply(k).apply(l)
                             else {
                                    val (r, s): (BigInt, BigInt) =
                                      ((k: BigInt) => (l: BigInt) => if (l == 0)
(BigInt(0), k) else (k.abs /% l.abs)).apply(k).apply(l);
                                    (if (s == BigInt(0)) ((- r), BigInt(0))
                                      else ((- r) - BigInt(1), l.abs - s))
                                  }))))

def nat_of_integer(k: BigInt): nat = Nat(max[BigInt](BigInt(0), k))

def mod_integer(k: BigInt, l: BigInt): BigInt =
  snd[BigInt, BigInt](divmod_integer(k, l))

def mod_nat(m: nat, n: nat): nat =
  Nat(mod_integer(integer_of_nat(m), integer_of_nat(n)))

def times_nat(m: nat, n: nat): nat = Nat(integer_of_nat(m) * integer_of_nat(n))

} /* object MPLib */

object Optiona {

def equal_optiona[A : MPLib.equal](x0: Option[A], x1: Option[A]): Boolean =
  (x0, x1) match {
  case (None, Some(x2)) => false
  case (Some(x2), None) => false
  case (Some(x2), Some(y2)) => MPLib.eq[A](x2, y2)
  case (None, None) => true
}

implicit def equal_option[A : MPLib.equal]: MPLib.equal[Option[A]] = new
  MPLib.equal[Option[A]] {
  val `MPLib.equal` = (a: Option[A], b: Option[A]) => equal_optiona[A](a, b)
}

} /* object Optiona */

object MultiPaxosImpl {

import /*implicits*/ MPLib.equal_prod, Optiona.equal_option, MPLib.equal_list,
  MPLib.ord_nat, MPLib.card_UNIV_nat, MPLib.linorder_nat, MPLib.equal_unit,
  MPLib.equal_nat

abstract sealed class cmd[A]
final case class Comd[A](a: A) extends cmd[A]
{
  override def equals(other: Any) = other match {
    case that:Comd[A] => (that.isInstanceOf[Comd[A]]) && this.a == that.a
    case _ => false
  }
  override def toString = "Comd(" + a.toString() + ")"
  override def hashCode : Int = a.hashCode()
}
final case class NoOp[A]() extends cmd[A]
{
  override def equals(other: Any) = other match {
    case that:NoOp[A] => (that.isInstanceOf[NoOp[A]])
    case _ => false
  }
  override def toString = "NoOp"
  override def hashCode : Int = 41
}

def equal_cmda[A : MPLib.equal](x0: cmd[A], x1: cmd[A]): Boolean = (x0, x1)
  match {
  case (Comd(x1), NoOp()) => false
  case (NoOp(), Comd(x1)) => false
  case (Comd(x1), Comd(y1)) => MPLib.eq[A](x1, y1)
  case (NoOp(), NoOp()) => true
}

implicit def equal_cmd[A : MPLib.equal]: MPLib.equal[cmd[A]] = new
  MPLib.equal[cmd[A]] {
  val `MPLib.equal` = (a: cmd[A], b: cmd[A]) => equal_cmda[A](a, b)
}

abstract sealed class consensus_ext[A, B]
final case class
  consensus_exta[A, B](a: Integer, b: Integer, c: List[Integer],
                        d: Integer, e: Option[cmd[A]], f: B)
  extends consensus_ext[A, B]
{
  override def equals(other: Any) = other match {
    case that:consensus_exta[A,B] => (that.isInstanceOf[consensus_exta[A,B]]) && this.a == that.a && this.b == that.b && this.c == that.c && this.d == that.d && this.e == that.e
    case _ => false
  }
  override def toString = " inst: " +a.toString() + "    view: " + b.toString() + ",    accepts: " + c.toString() + ",    status: " + d.toString() + ",    value:" + e.toString() + "\n"
  override def hashCode : Int =   41 * (  41 * (  41 * (  41 * (  41 * (1) + a.hashCode()) + b.hashCode()) + c.hashCode()) + d.hashCode()) + e.hashCode()
}

def equal_int(a:Integer, b:Integer) : Boolean = (a == b)
def times_int(a:Integer, b:Integer) : Integer = (a * b)
def less_int(a:Integer, b:Integer) : Boolean = a < b
def less_eq_int(a:Integer, b:Integer) : Boolean = a <= b
def minus_int(a:Integer, b:Integer) : Integer = a - b
def plus_int(a:Integer, b:Integer) : Integer = a + b
def mod_int(a:Integer, b:Integer) : Integer = a % b
def divide_int(a:Integer, b:Integer) : Integer = (a / b)
def suc_int(a:Integer) : Integer = (a + 1)
def upt_int(i: Integer, j: Integer): List[Integer] =
  (if (i < j) i :: upt_int(i + 1, j) else Nil)

def remove_list[A](x: A, xa1: List[A]): List[A] = (x, xa1) match {
  case (x, Nil) => Nil
  case (x, y :: xs) => (if (x == y) remove_list[A](x, xs) else y :: remove_list[A](x, xs))
}
def minus_list[A](a: List[A], x1: List[A]): List[A] = {
    MPLib.fold[A, List[A]](((aa: A) => (b: List[A]) => remove_list[A](aa, b)), x1, a)
}
def member_list[A](x0: List[A], y: A): Boolean = (x0, y) match {
  case (Nil, y) => false
  case (x :: xs, y) => (x == y) || member_list[A](xs, y)
}
def insert_list[A](x: A, xs: List[A]): List[A] = {
  (if (member_list[A](xs, x)) xs else x :: xs)
}
def sup_list[A](x0: List[A], a: List[A]): List[A] = {
    MPLib.fold[A, List[A]](((aa: A) => (b: List[A]) => insert_list[A](aa, b)), x0, a)
}

def list_nth[A](x0: List[A], a: Integer): A = {x0.apply(a)}

def getCmdVal[A](a : cmd[A]) : A = a match
{
  case(Comd(x1)) => x1
}

def getFwdFields[A](message : msg[A]) : A = message match {
  case(Fwd(a)) => a
}

def getFields[A](pack : packet[A]) : (Integer, Integer, msg[A]) = pack match {
  case(Packet(a,b,c)) => (a,b,c)
}

def equal_consensus_exta[A : MPLib.equal,
                          B : MPLib.equal](x0: consensus_ext[A, B],
    x1: consensus_ext[A, B]):
      Boolean
  =
  (x0, x1) match {
  case (consensus_exta(insa, viewa, acceptsa, statusa, valaa, morea),
         consensus_exta(ins, view, accepts, status, vala, more))
    => equal_int(insa, ins) &&
         (equal_int(viewa, view) &&
           (MPLib.equal_lista[Integer](acceptsa, accepts) &&
             (equal_int(statusa, status) &&
               (Optiona.equal_optiona[cmd[A]](valaa, vala) &&
                 MPLib.eq[B](morea, more)))))
}

implicit def
  equal_consensus_ext[A : MPLib.equal, B : MPLib.equal]:
    MPLib.equal[consensus_ext[A, B]]
  = new MPLib.equal[consensus_ext[A, B]] {
  val `MPLib.equal` = (a: consensus_ext[A, B], b: consensus_ext[A, B]) =>
    equal_consensus_exta[A, B](a, b)
}

abstract sealed class msg[A]
final case class Phase1a[A](a: Integer, b: Integer) extends msg[A]
{
  override def equals(other: Any) = other match {
    case that:Phase1a[A] => (that.isInstanceOf[Phase1a[A]]) && this.a == that.a && this.b == that.b
    case _ => false
  }
  override def toString = "Phase1a(" + a.toString() + ", " + b.toString() + ")"
  override def hashCode : Int =   41 * (  41 * (1) + a.hashCode()) + b.hashCode()
}
final case class Phase1b[A](a: List[consensus_ext[A, Unit]], b: Integer)
  extends msg[A]
{
  override def equals(other: Any) = other match {
    case that:Phase1b[A] => (that.isInstanceOf[Phase1b[A]]) && this.a == that.a && this.b == that.b
    case _ => false
  }
  override def toString = "Phase1b(" + a.toString() + ", " + b.toString() + ")"
  override def hashCode : Int =   41 * (  41 * (1) + a.hashCode()) + b.hashCode()
}
final case class Phase2a[A](a: Integer, b: Integer, c: cmd[A]) extends
  msg[A]
{
  override def equals(other: Any) = other match {
    case that:Phase2a[A] => (that.isInstanceOf[Phase2a[A]]) && this.a == that.a && this.b == that.b && this.c == that.c
    case _ => false
  }
  override def toString = "Phase2a(" + a.toString() + ", " + b.toString() + ", " + c.toString() + ")"
  override def hashCode : Int =   41 * (  41 * (  41 * (1) + a.hashCode()) + b.hashCode()) + c.hashCode()
}
final case class Phase2b[A](a: Integer, b: Integer, c: cmd[A]) extends
  msg[A]
{
  override def equals(other: Any) = other match {
    case that:Phase2b[A] => (that.isInstanceOf[Phase2b[A]]) && this.a == that.a && this.b == that.b && this.c == that.c
    case _ => false
  }
  override def toString = "Phase2b(" + a.toString() + ", " + b.toString() + ", " + c.toString() + ")"
  override def hashCode : Int =   41 * (  41 * (  41 * (1) + a.hashCode()) + b.hashCode()) + c.hashCode()
}
final case class Fwd[A](a: A) extends msg[A]
{
  override def equals(other: Any) = other match {
    case that:Fwd[A] => (that.isInstanceOf[Fwd[A]]) && this.a == that.a
    case _ => false
  }
  override def toString = "Fwd(" + a.toString() + ")"
  override def hashCode : Int = a.hashCode()
}

abstract sealed class packet[A]
final case class Packet[A](a: Integer, b: Integer, c: msg[A]) extends
  packet[A]
{
  override def equals(other: Any) = other match {
    case that:Packet[A] => (that.isInstanceOf[Packet[A]]) && this.a == that.a && this.b == that.b && this.c == that.c
    case _ => false
  }
  override def toString = "Packet(" + a.toString() + ", " + b.toString() + ", " + c.toString() + ")"
  override def hashCode : Int =   41 * (  41 * (  41 * (1) + a.hashCode()) + b.hashCode()) + c.hashCode()
}

abstract sealed class state_ext[A, B]
final case class
  state_exta[A, B](a: Integer, b: Boolean, c: List[Integer], d: Integer,
                    e: Integer,
                    f: MPLib.finfun[Integer,
                                     List[(Integer,
    (Integer, Option[cmd[A]]))]],
                    g: Integer,
                    h: MPLib.finfun[Integer, consensus_ext[A, Unit]], i: B)
  extends state_ext[A, B]
{
  override def equals(other: Any) = other match {
    case that:state_exta[A,B] => (that.isInstanceOf[state_exta[A,B]]) && this.a == that.a && this.b == that.b && this.c == that.c && this.d == that.d && this.e == that.e && this.f == that.f && this.g == that.g && this.h == that.h
    case _ => false
  }
  override def toString = "id: " + a.toString() + "    leader: " + b.toString() + ",    acceptors: " + c.toString() + ",    ballot: " + d.toString() + ",    firstUncommitted: " + e.toString() + ",    onebs: " + f.toString() + "\n" + "    next_inst: " + g.toString() + ",    instances: " + h.toString() + "\n"
  override def hashCode : Int =   41 * (  41 * (  41 * (  41 * (  41 * (  41 * (  41 * (  41 * (1) + a.hashCode()) + b.hashCode()) + c.hashCode()) + d.hashCode()) + e.hashCode()) + f.hashCode()) + g.hashCode()) + h.hashCode()
}

def accs(n: Integer): List[Integer] =
  (if (equal_int(n, 0)) Nil
    else accs(minus_int(n, 1)) ++
           List(minus_int(n, 1)))

def emptyOBS[A]:
      MPLib.finfun[Integer, List[(Integer, (Integer, Option[cmd[A]]))]]
  =
  MPLib.finfun_const[List[(Integer, (Integer, Option[cmd[A]]))],
                      Integer](Nil)

def acceptors[A, B](x0: state_ext[A, B]): List[Integer] = x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => acceptors
}

def send_all[A, B, C](acc: Integer, mesg: msg[A], s: state_ext[B, C]):
      List[packet[A]]
  =
  MPLib.map[Integer,
             packet[A]](((a2: Integer) => Packet[A](acc, a2, mesg)),
                         MPLib.remove1[Integer](acc, acceptors[B, C](s)))

def id[A, B](x0: state_ext[A, B]): Integer = x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => id
}

def instances[A, B](x0: state_ext[A, B]):
      MPLib.finfun[Integer, consensus_ext[A, Unit]]
  =
  x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => instances
}

def vala[A, B](x0: consensus_ext[A, B]): Option[cmd[A]] = x0 match {
  case consensus_exta(ins, view, accepts, status, vala, more) => vala
}

def def_getRequest[A](i: Integer, s: state_ext[A, Unit]): Option[cmd[A]] =
  vala[A, Unit](MPLib.finfun_apply[Integer,
                                    consensus_ext[A,
           Unit]](instances[A, Unit](s), i))

def def_learn[A : MPLib.equal](i: Integer, v: A, s: state_ext[A, Unit]):
      Option[(state_ext[A, Unit], List[packet[A]])]
  =
  (def_getRequest[A](i, s) match {
     case None => None
     case Some(Comd(c)) =>
       (if (MPLib.eq[A](v, c))
         Some[(state_ext[A, Unit], List[packet[A]])]((s, Nil)) else None)
     case Some(NoOp()) => None
   })

def ins[A, B](x0: consensus_ext[A, B]): Integer = x0 match {
  case consensus_exta(ins, view, accepts, status, vala, more) => ins
}

def def_getIns[A, B](cs: consensus_ext[A, B]): Integer = ins[A, B](cs)

def firstUncommitted[A, B](x0: state_ext[A, B]): Integer = x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => firstUncommitted
}

def ballot_update[A, B](ballota: Integer => Integer, x1: state_ext[A, B]):
      state_ext[A, B]
  =
  (ballota, x1) match {
  case (ballota,
         state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                     next_inst, instances, more))
    => state_exta[A, B](id, leader, acceptors, ballota(ballot),
                         firstUncommitted, onebs, next_inst, instances, more)
}

def onebs_update[A, B](onebsa:
                         (MPLib.finfun[Integer,
List[(Integer, (Integer, Option[cmd[A]]))]]) =>
                           MPLib.finfun[Integer,
 List[(Integer, (Integer, Option[cmd[A]]))]],
                        x1: state_ext[A, B]):
      state_ext[A, B]
  =
  (onebsa, x1) match {
  case (onebsa,
         state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                     next_inst, instances, more))
    => state_exta[A, B](id, leader, acceptors, ballot, firstUncommitted,
                         onebsa(onebs), next_inst, instances, more)
}

def def_replicaCount[A, B](s: state_ext[A, B]): Integer =
  (acceptors[A, B](s)).length

def ballot[A, B](x0: state_ext[A, B]): Integer = x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => ballot
}

def generateBallot(a: Integer, b: Integer, n: Integer): Integer =
  (if (equal_int(b, 0)) 0
    else (if (equal_int(mod_int(suc_int(minus_int(b,
                                1)),
      n),
                                a))
           suc_int(minus_int(b, 1))
           else generateBallot(a, minus_int(b, 1), n)))

def nextBallot(a: Integer, b: Integer, n: Integer): Integer =
  generateBallot(a, plus_int(b, n), n)

def def_send1a[A](s: state_ext[A, Unit]): (state_ext[A, Unit], List[packet[A]])
  =
  {
    val a: Integer = id[A, Unit](s)
    val b: Integer =
      nextBallot(a, ballot[A, Unit](s), def_replicaCount[A, Unit](s))
    val i: Integer = firstUncommitted[A, Unit](s)
    val msg_1a: msg[A] = Phase1a[A](b, i);
    (onebs_update[A, Unit](((_: MPLib.finfun[Integer,
      List[(Integer, (Integer, Option[cmd[A]]))]])
                              =>
                             MPLib.finfun_const[List[(Integer,
               (Integer, Option[cmd[A]]))],
         Integer](Nil)),
                            ballot_update[A, Unit](((_: Integer) => b), s)),
      send_all[A, A, Unit](a, msg_1a, s))
  }

def accepts_update[A, B](acceptsa: (List[Integer]) => List[Integer],
                          x1: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  (acceptsa, x1) match {
  case (acceptsa, consensus_exta(ins, view, accepts, status, vala, more)) =>
    consensus_exta[A, B](ins, view, acceptsa(accepts), status, vala, more)
}

def status_update[A, B](statusa: Integer => Integer,
                         x1: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  (statusa, x1) match {
  case (statusa, consensus_exta(ins, view, accepts, status, vala, more)) =>
    consensus_exta[A, B](ins, view, accepts, statusa(status), vala, more)
}

def next_inst_update[A, B](next_insta: Integer => Integer,
                            x1: state_ext[A, B]):
      state_ext[A, B]
  =
  (next_insta, x1) match {
  case (next_insta,
         state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                     next_inst, instances, more))
    => state_exta[A, B](id, leader, acceptors, ballot, firstUncommitted, onebs,
                         next_insta(next_inst), instances, more)
}

def instances_update[A, B](instancesa:
                             (MPLib.finfun[Integer,
    consensus_ext[A, Unit]]) =>
                               MPLib.finfun[Integer, consensus_ext[A, Unit]],
                            x1: state_ext[A, B]):
      state_ext[A, B]
  =
  (instancesa, x1) match {
  case (instancesa,
         state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                     next_inst, instances, more))
    => state_exta[A, B](id, leader, acceptors, ballot, firstUncommitted, onebs,
                         next_inst, instancesa(instances), more)
}

def view_update[A, B](viewa: Integer => Integer, x1: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  (viewa, x1) match {
  case (viewa, consensus_exta(ins, view, accepts, status, vala, more)) =>
    consensus_exta[A, B](ins, viewa(view), accepts, status, vala, more)
}

def val_update[A, B](vala: Option[cmd[A]] => Option[cmd[A]],
                      x1: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  (vala, x1) match {
  case (valaa, consensus_exta(ins, view, accepts, status, vala, more)) =>
    consensus_exta[A, B](ins, view, accepts, status, valaa(vala), more)
}

def ins_update[A, B](insa: Integer => Integer, x1: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  (insa, x1) match {
  case (insa, consensus_exta(ins, view, accepts, status, vala, more)) =>
    consensus_exta[A, B](insa(ins), view, accepts, status, vala, more)
}

def next_inst[A, B](x0: state_ext[A, B]): Integer = x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => next_inst
}

def def_send2a[A : MPLib.equal, B](i: Integer, v: cmd[A], s: state_ext[A, B]):
      (state_ext[A, B], List[packet[A]])
  =
  {
    val a: Integer = id[A, B](s)
    val inst: Integer = next_inst[A, B](s)
    val b: Integer = ballot[A, B](s)
    val msg: msg[A] = Phase2a[A](inst, b, v)
    val new_state: state_ext[A, B] =
      instances_update[A, B](((_: MPLib.finfun[Integer,
        consensus_ext[A, Unit]])
                                =>
                               MPLib.finfun_update[Integer,
            consensus_ext[A, Unit]](instances[A, B](s), i,
                                     val_update[A,
         Unit](((_: Option[cmd[A]]) => Some[cmd[A]](v)),
                status_update[A, Unit](((_: Integer) => 1),
accepts_update[A, Unit](((_: List[Integer]) => List(a)),
                         view_update[A, Unit](((_: Integer) => b),
       ins_update[A, Unit](((_: Integer) => i),
                            MPLib.finfun_apply[Integer,
        consensus_ext[A, Unit]](instances[A, B](s), i)))))))),
                              next_inst_update[A,
        B](((_: Integer) =>
             (if (less_int(plus_int(i, 1), inst)) inst
               else plus_int(i, 1))),
            s));
    (new_state, send_all[A, A, B](a, msg, s))
  }

def def_setIns[A, B](i: Integer, cs: consensus_ext[A, B]): consensus_ext[A, B]
  =
  ins_update[A, B](((_: Integer) => i), cs)

def init_state[A](n: Integer, a: Integer): state_ext[A, Unit] =
  state_exta[A, Unit](a, false, accs(n), 0, 1,
                       MPLib.finfun_const[List[(Integer,
         (Integer, Option[cmd[A]]))],
   Integer](Nil),
                       1,
                       MPLib.finfun_const[consensus_ext[A, Unit],
   Integer](consensus_exta[A, Unit](0, 0, Nil,
                                       0, None, ())),
                       ())

def addInstance[A : MPLib.equal](i: Integer,
                                  nConsensus: consensus_ext[A, Unit],
                                  insts:
                                    MPLib.finfun[Integer,
          consensus_ext[A, Unit]]):
      MPLib.finfun[Integer, consensus_ext[A, Unit]]
  =
  MPLib.finfun_update[Integer, consensus_ext[A, Unit]](insts, i, nConsensus)

def view[A, B](x0: consensus_ext[A, B]): Integer = x0 match {
  case consensus_exta(ins, view, accepts, status, vala, more) => view
}

def def_getView[A, B](cs: consensus_ext[A, B]): Integer = view[A, B](cs)

def def_leaderOfBal(b: Integer, n: Integer): Integer =
  (if (equal_int(b, 0)) 0
    else mod_int(suc_int(minus_int(b, 1)), n))

def leader[A, B](x0: state_ext[A, B]): Boolean = x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => leader
}

def def_propose[A : MPLib.equal](v: A, s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  {
    val a: Integer = id[A, Unit](s);
    (if (leader[A, Unit](s))
      def_send2a[A, Unit](next_inst[A, Unit](s), Comd[A](v), s)
      else (s, List(Packet[A](a, def_leaderOfBal(ballot[A, Unit](s),
          def_replicaCount[A, Unit](s)),
                               Fwd[A](v)))))
  }

def def_setView[A, B](b: Integer, cs: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  view_update[A, B](((_: Integer) => b), cs)

def finfun_filt[A : MPLib.card_UNIV : MPLib.equal : MPLib.linorder,
                 B : MPLib.equal](ff: MPLib.finfun[A, B], filt: A => Boolean):
      MPLib.finfun[A, B]
  =
  MPLib.fold[A, MPLib.finfun[A, B]](((k: A) => (df: MPLib.finfun[A, B]) =>
                                      (if (filt(k)) df
else MPLib.finfun_update[A, B](df, k, MPLib.finfun_apply[A, B](ff, k)))),
                                     MPLib.finfun_to_list[A, B](ff),
                                     MPLib.finfun_const[B,
                 A](MPLib.finfun_default[A, B](ff)))

def onebs[A, B](x0: state_ext[A, B]):
      MPLib.finfun[Integer, List[(Integer, (Integer, Option[cmd[A]]))]]
  =
  x0 match {
  case state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                   next_inst, instances, more)
    => onebs
}

def def_getInsts[A : MPLib.card_UNIV : MPLib.equal : MPLib.linorder,
                  B : MPLib.equal](insts: MPLib.finfun[A, B]):
      List[A]
  =
  MPLib.finfun_to_list[A, B](insts)

def def_getValue[A, B](cs: consensus_ext[A, B]): Option[cmd[A]] = vala[A, B](cs)

def def_isLeader[A, B](s: state_ext[A, B]): Boolean = leader[A, B](s)

def firstUncommitted_update[A, B](firstUncommitteda: Integer => Integer,
                                   x1: state_ext[A, B]):
      state_ext[A, B]
  =
  (firstUncommitteda, x1) match {
  case (firstUncommitteda,
         state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                     next_inst, instances, more))
    => state_exta[A, B](id, leader, acceptors, ballot,
                         firstUncommitteda(firstUncommitted), onebs, next_inst,
                         instances, more)
}

def def_receive2_first[A : MPLib.equal](i: Integer, b: Integer, v: cmd[A],
 l: Integer, s: state_ext[A, Unit]):
      state_ext[A, Unit]
  =
  {
    val bal: Integer = ballot[A, Unit](s);
    (if (less_eq_int(bal, b))
      {
        val a: Integer = id[A, Unit](s)
        val nas: Integer = def_replicaCount[A, Unit](s)
        val nextInst: Integer = next_inst[A, Unit](s)
        val nInst: Integer =
          (if (less_int(plus_int(i, 1), nextInst))
            nextInst else plus_int(i, 1))
        val fUncommitted: Integer = firstUncommitted[A, Unit](s)
        val fUndecide: Integer =
          (if (less_int(plus_int(i, 1), fUncommitted))
            fUncommitted else plus_int(i, 1));
        (if (less_eq_int(((4)), nas))
          instances_update[A, Unit](((_:
MPLib.finfun[Integer, consensus_ext[A, Unit]])
                                       =>
                                      MPLib.finfun_update[Integer,
                   consensus_ext[A, Unit]](instances[A, Unit](s), i,
    val_update[A, Unit](((_: Option[cmd[A]]) => Some[cmd[A]](v)),
                         status_update[A,
Unit](((_: Integer) => 1),
       accepts_update[A, Unit](((_: List[Integer]) => List(a, l)),
                                view_update[A,
     Unit](((_: Integer) => b),
            ins_update[A, Unit](((_: Integer) => i),
                                 MPLib.finfun_apply[Integer,
             consensus_ext[A, Unit]](instances[A, Unit](s), i)))))))),
                                     next_inst_update[A,
               Unit](((_: Integer) => nInst),
                      ballot_update[A, Unit](((_: Integer) => b), s)))
          else instances_update[A, Unit](((_:
     MPLib.finfun[Integer, consensus_ext[A, Unit]])
    =>
   MPLib.finfun_update[Integer,
                        consensus_ext[A, Unit]](instances[A, Unit](s), i,
         val_update[A, Unit](((_: Option[cmd[A]]) => Some[cmd[A]](v)),
                              status_update[A,
     Unit](((_: Integer) => ((2))),
            accepts_update[A, Unit](((_: List[Integer]) => List(a, l)),
                                     view_update[A,
          Unit](((_: Integer) => b),
                 ins_update[A, Unit](((_: Integer) => i),
                                      MPLib.finfun_apply[Integer,
                  consensus_ext[A, Unit]](instances[A, Unit](s), i)))))))),
  firstUncommitted_update[A, Unit](((_: Integer) => fUndecide),
                                    next_inst_update[A,
              Unit](((_: Integer) => nInst),
                     ballot_update[A, Unit](((_: Integer) => b), s)))))
      }
      else s)
  }

def accepts[A, B](x0: consensus_ext[A, B]): List[Integer] = x0 match {
  case consensus_exta(ins, view, accepts, status, vala, more) => accepts
}

def def_receive2_addl[A : MPLib.equal](i: Integer, b: Integer,
a2: Integer, s: state_ext[A, Unit]):
      state_ext[A, Unit]
  =
  {
    id[A, Unit](s)
    val accs: List[Integer] =
      accepts[A, Unit](MPLib.finfun_apply[Integer,
   consensus_ext[A, Unit]](instances[A, Unit](s), i))
    val nas: Integer = def_replicaCount[A, Unit](s);
    (if (MPLib.membera[Integer](accs, a2)) s
      else {
             val newaccs: List[Integer] = a2 :: accs
             val votes: Integer = (newaccs).length;
             (if (less_eq_int(times_int(((2)),
             votes),
                                     nas))
               instances_update[A, Unit](((_:
     MPLib.finfun[Integer, consensus_ext[A, Unit]])
    =>
   MPLib.finfun_update[Integer,
                        consensus_ext[A, Unit]](instances[A, Unit](s), i,
         accepts_update[A, Unit](((_: List[Integer]) => newaccs),
                                  MPLib.finfun_apply[Integer,
              consensus_ext[A, Unit]](instances[A, Unit](s), i)))),
  s)
               else {
                      val fUncommitted: Integer =
                        firstUncommitted[A, Unit](s);
                      instances_update[A,
Unit](((_: MPLib.finfun[Integer, consensus_ext[A, Unit]]) =>
        MPLib.finfun_update[Integer,
                             consensus_ext[A,
    Unit]](instances[A, Unit](s), i,
            status_update[A, Unit](((_: Integer) =>
                                     ((2))),
                                    accepts_update[A,
            Unit](((_: List[Integer]) => newaccs),
                   MPLib.finfun_apply[Integer,
                                       consensus_ext[A,
              Unit]](instances[A, Unit](s), i))))),
       firstUncommitted_update[A, Unit](((_: Integer) =>
  (if (less_int(plus_int(i, 1), fUncommitted))
    fUncommitted else plus_int(i, 1))),
 s))
                    })
           })
  }

def status[A, B](x0: consensus_ext[A, B]): Integer = x0 match {
  case consensus_exta(ins, view, accepts, status, vala, more) => status
}

def def_getStatus[A, B](cs: consensus_ext[A, B]): Integer = status[A, B](cs)

def def_receive2[A : MPLib.equal](i: Integer, b: Integer, v: cmd[A],
                                   l: Integer, s: state_ext[A, Unit]):
      state_ext[A, Unit]
  =
  {
    val i_status: Integer =
      def_getStatus[A, Unit](MPLib.finfun_apply[Integer,
         consensus_ext[A, Unit]](instances[A, Unit](s), i));
    (if (less_int(0, i_status))
      def_receive2_addl[A](i, b, l, s)
      else def_receive2_first[A](i, b, v, l, s))
  }

def def_setValue[A, B](v: Option[cmd[A]], cs: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  val_update[A, B](((_: Option[cmd[A]]) => v), cs)

def construct_msg[A](x0: List[Option[cmd[A]]]): List[cmd[A]] = x0 match {
  case Nil => Nil
  case None :: xs => NoOp[A]() :: construct_msg[A](xs)
  case Some(x) :: xs => x :: construct_msg[A](xs)
}

def def_getBallot[A, B](s: state_ext[A, B]): Integer = ballot[A, B](s)

def def_getLeader[A, B](s: state_ext[A, B]): Option[Integer] =
  (if (equal_int(ballot[A, B](s), 0)) None
    else Some[Integer](mod_int(suc_int(minus_int(ballot[A,
                                 B](s),
                          1)),
def_replicaCount[A, B](s))))

def def_isDecided[A, B](i: Integer, s: state_ext[A, B]): Boolean =
  equal_int(status[A, Unit](MPLib.finfun_apply[Integer,
               consensus_ext[A, Unit]](instances[A, B](s), i)),
                    ((2)))

def leader_update[A, B](leadera: Boolean => Boolean, x1: state_ext[A, B]):
      state_ext[A, B]
  =
  (leadera, x1) match {
  case (leadera,
         state_exta(id, leader, acceptors, ballot, firstUncommitted, onebs,
                     next_inst, instances, more))
    => state_exta[A, B](id, leadera(leader), acceptors, ballot,
                         firstUncommitted, onebs, next_inst, instances, more)
}

def finfun_serialize[A : MPLib.card_UNIV : MPLib.equal : MPLib.linorder,
                      B : MPLib.equal](vs: MPLib.finfun[A, B]):
      List[B]
  =
  {
    val insts: List[A] = MPLib.finfun_to_list[A, B](vs)
    val ilength: Integer = (insts).length
    val a: List[Integer] = upt_int(0, ilength);
    MPLib.map[Integer,
               B](((i: Integer) =>
                    MPLib.finfun_apply[A, B](vs, list_nth[A](insts, i))),
                   a)
  }

def finfun_filt_l[A : MPLib.card_UNIV : MPLib.equal : MPLib.linorder,
                   B : MPLib.equal](ff: MPLib.finfun[A, B], truncloc: A):
      MPLib.finfun[A, B]
  =
  finfun_filt[A, B](ff, ((k: A) => MPLib.less[A](k, truncloc)))

def def_receive1a[A : MPLib.equal](l: Integer, b: Integer, i: Integer,
                                    s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  {
    val bal: Integer = ballot[A, Unit](s);
    (if (less_eq_int(bal, b))
      {
        val a: Integer = id[A, Unit](s)
        val insts: List[consensus_ext[A, Unit]] =
          finfun_serialize[Integer,
                            consensus_ext[A,
   Unit]](finfun_filt_l[Integer,
                         consensus_ext[A, Unit]](instances[A, Unit](s), i))
        val msg_1b: msg[A] = Phase1b[A](insts, b)
        val packet: packet[A] = Packet[A](a, l, msg_1b)
        val state: state_ext[A, Unit] =
          onebs_update[A, Unit](((_: MPLib.finfun[Integer,
           List[(Integer, (Integer, Option[cmd[A]]))]])
                                   =>
                                  MPLib.finfun_const[List[(Integer,
                    (Integer, Option[cmd[A]]))],
              Integer](Nil)),
                                 leader_update[A,
        Unit](((_: Boolean) => false),
               ballot_update[A, Unit](((_: Integer) => b), s)));
        (state, List(packet))
      }
      else (s, Nil))
  }

def finfun_deserialize[A](vs: List[consensus_ext[A, Unit]]):
      MPLib.finfun[Integer, consensus_ext[A, Unit]]
  =
  (MPLib.foldr[consensus_ext[A, Unit],
                MPLib.finfun[Integer,
                              consensus_ext[A,
     Unit]]](((kv: consensus_ext[A, Unit]) =>
               (r: MPLib.finfun[Integer, consensus_ext[A, Unit]]) =>
               MPLib.finfun_update_code[Integer,
 consensus_ext[A, Unit]](r, view[A, Unit](kv), kv)),
              vs)).apply(MPLib.finfun_const[consensus_ext[A, Unit],
     Integer](consensus_exta[A, Unit](0, 0, Nil,
 0, None, ())))

def update_consensus[A, B,
                      C](newc: consensus_ext[A, B], c: consensus_ext[A, C],
                          nas: Integer):
      consensus_ext[A, C]
  =
  (if (equal_int(status[A, C](c), ((2)))) c
    else (if (equal_int(status[A, B](newc),
                                ((2))))
           status_update[A, C](((_: Integer) =>
                                 ((2))),
                                val_update[A,
    C](((_: Option[cmd[A]]) => vala[A, B](newc)),
        view_update[A, C](((_: Integer) => view[A, B](newc)),
                           ins_update[A, C](((_: Integer) => ins[A, B](newc)),
     c))))
           else (if (less_int(view[A, C](c), view[A, B](newc)))
                  {
                    val c1: consensus_ext[A, C] =
                      val_update[A, C](((_: Option[cmd[A]]) =>
 vala[A, B](newc)),
accepts_update[A, C](((_: List[Integer]) =>
                       MPLib.union[Integer].apply(accepts[A,
                     C](c)).apply(accepts[A, B](newc))),
                      view_update[A, C](((_: Integer) => view[A, B](newc)),
 ins_update[A, C](((_: Integer) => ins[A, B](newc)), c))));
                    (if (less_int(nas,
 times_int(((2)), (accepts[A, C](c1)).length)))
                      status_update[A, C](((_: Integer) =>
    ((2))),
   c1)
                      else c1)
                  }
                  else {
                         val c1: consensus_ext[A, C] =
                           accepts_update[A,
   C](((_: List[Integer]) =>
        MPLib.union[Integer].apply(accepts[A,
      C](c)).apply(accepts[A, B](newc))),
       c);
                         (if (less_int(nas,
      times_int(((2)), (accepts[A, C](c1)).length)))
                           status_update[A,
  C](((_: Integer) => ((2))), c1)
                           else c1)
                       })))

def update_instance[A : MPLib.equal](s: state_ext[A, Unit], a: Integer,
                                      last_vs: List[consensus_ext[A, Unit]]):
      state_ext[A, Unit]
  =
  {
    val lastvs: MPLib.finfun[Integer, consensus_ext[A, Unit]] =
      finfun_deserialize[A](last_vs)
    val newInsts:
          ((consensus_ext[A, Unit], consensus_ext[A, Unit])) =>
            consensus_ext[A, Unit]
      = ((aa: (consensus_ext[A, Unit], consensus_ext[A, Unit])) =>
          {
            val (newc, c): (consensus_ext[A, Unit], consensus_ext[A, Unit]) =
              aa;
            update_consensus[A, Unit,
                              Unit](newc, c, def_replicaCount[A, Unit](s))
          })
    val pair_insts:
          MPLib.finfun[Integer,
                        (consensus_ext[A, Unit], consensus_ext[A, Unit])]
      = MPLib.finfun_Diag[Integer, consensus_ext[A, Unit],
                           consensus_ext[A,
  Unit]](lastvs, instances[A, Unit](s))
    val new_instances: MPLib.finfun[Integer, consensus_ext[A, Unit]] =
      MPLib.finfun_comp[(consensus_ext[A, Unit], consensus_ext[A, Unit]),
                         consensus_ext[A, Unit],
                         Integer](newInsts, pair_insts)
    val instance_list: List[Integer] =
      MPLib.finfun_to_list[Integer, consensus_ext[A, Unit]](new_instances)
    val undecided: Integer =
      (if (MPLib.nulla[Integer](instance_list)) 0
        else list_nth[Integer](MPLib.filter[Integer](((i: Integer) =>
                    less_int(status[A,
   Unit](MPLib.finfun_apply[Integer,
                             consensus_ext[A, Unit]](new_instances, i)),
                                    ((2)))),
                   instance_list),
                                   0))
    val fUncommitted: Integer = firstUncommitted[A, Unit](s)
    val combiner:
          ((List[(Integer, (Integer, Option[cmd[A]]))],
            consensus_ext[A, Unit])) =>
            List[(Integer, (Integer, Option[cmd[A]]))]
      = ((b: (List[(Integer, (Integer, Option[cmd[A]]))],
               consensus_ext[A, Unit]))
           =>
          {
            val (xs, c):
                  (List[(Integer, (Integer, Option[cmd[A]]))],
                    consensus_ext[A, Unit])
              = b
            val vs: (Integer, Option[cmd[A]]) =
              (view[A, Unit](c), vala[A, Unit](c));
            (if (MPLib.membera[(Integer,
                                 (Integer, Option[cmd[A]]))](xs, (a, vs)))
              xs else (a, vs) :: xs)
          })
    val pair_map:
          MPLib.finfun[Integer,
                        (List[(Integer, (Integer, Option[cmd[A]]))],
                          consensus_ext[A, Unit])]
      = MPLib.finfun_Diag[Integer,
                           List[(Integer, (Integer, Option[cmd[A]]))],
                           consensus_ext[A, Unit]](onebs[A, Unit](s), lastvs)
    val new_onebs:
          MPLib.finfun[Integer,
                        List[(Integer, (Integer, Option[cmd[A]]))]]
      = MPLib.finfun_comp[(List[(Integer, (Integer, Option[cmd[A]]))],
                            consensus_ext[A, Unit]),
                           List[(Integer, (Integer, Option[cmd[A]]))],
                           Integer](combiner, pair_map);
    firstUncommitted_update[A, Unit](((_: Integer) =>
                                       (if (less_int(undecided,
                    fUncommitted))
 fUncommitted else undecided)),
                                      onebs_update[A,
            Unit](((_: MPLib.finfun[Integer,
                                     List[(Integer,
    (Integer, Option[cmd[A]]))]])
                     =>
                    new_onebs),
                   instances_update[A, Unit](((_:
         MPLib.finfun[Integer, consensus_ext[A, Unit]])
        =>
       new_instances),
      s)))
  }

def quorum_received[A, B](i: Integer, s: state_ext[A, B]): Boolean =
  {
    val at_b_i: List[(Integer, (Integer, Option[cmd[A]]))] =
      MPLib.finfun_apply[Integer,
                          List[(Integer,
                                 (Integer,
                                   Option[cmd[A]]))]](onebs[A, B](s), i);
    less_int(def_replicaCount[A, B](s),
                    times_int(((2)), (at_b_i).length))
  }

def highest_voted[A : MPLib.equal, B, C : MPLib.ord,
                   D](onebs_bal: MPLib.finfun[A, List[(B, (C, Option[D]))]]):
      A => Option[D]
  =
  {
    val onebs_i: A => List[(C, Option[D])] =
      ((i: A) =>
        MPLib.map[(B, (C, Option[D])),
                   (C, Option[D])](((a: (B, (C, Option[D]))) =>
                                     MPLib.snd[B, (C, Option[D])](a)),
                                    MPLib.finfun_apply[A,
                List[(B, (C, Option[D]))]](onebs_bal, i)))
    val highest: (List[(C, Option[D])]) => Option[D] =
      ((bcl: List[(C, Option[D])]) =>
        (if (MPLib.nulla[(C, Option[D])](bcl)) None
          else MPLib.snd[C, Option[D]](MPLib.fold[(C, Option[D]),
           (C, Option[D])](((bc: (C, Option[D])) => (bc0: (C, Option[D])) =>
                             (if (MPLib.less[C](MPLib.fst[C, Option[D]](bc0),
         MPLib.fst[C, Option[D]](bc)))
                               bc else bc0)),
                            bcl,
                            list_nth[(C, Option[D])](bcl, 0)))));
    MPLib.comp[List[(C, Option[D])], Option[D], A](highest, onebs_i)
  }

def def_receive1b[A : MPLib.equal](last_vs: List[consensus_ext[A, Unit]],
                                    bal: Integer, a2: Integer,
                                    s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  (if (equal_int(bal, ballot[A, Unit](s)))
    {
      val a: Integer = id[A, Unit](s)
      val s1: state_ext[A, Unit] = update_instance[A](s, a2, last_vs);
      (if (quorum_received[A, Unit](firstUncommitted[A, Unit](s), s1))
        {
          val onebs_bal:
                MPLib.finfun[Integer,
                              List[(Integer, (Integer, Option[cmd[A]]))]]
            = onebs[A, Unit](s1)
          val max_i: Integer =
            {
              val l: List[Integer] =
                MPLib.finfun_to_list[Integer,
                                      List[(Integer,
     (Integer, Option[cmd[A]]))]](onebs_bal);
              (if (MPLib.nulla[Integer](l)) 0
                else MPLib.hd[Integer](MPLib.rev[Integer](l)))
            }
          val maxInst: Integer = next_inst[A, Unit](s1)
          val s2: state_ext[A, Unit] =
            next_inst_update[A, Unit](((_: Integer) =>
(if (less_int(plus_int(max_i, 1), maxInst)) maxInst
  else plus_int(max_i, 1))),
                                       leader_update[A,
              Unit](((_: Boolean) => true), s1))
          val startI: Integer = firstUncommitted[A, Unit](s)
          val insts: List[Integer] =
            upt_int(startI, plus_int(max_i, 1))
          val highestVoted: Integer => Option[cmd[A]] =
            highest_voted[Integer, Integer, Integer, cmd[A]](onebs_bal)
          val cmdOptions: List[Option[cmd[A]]] =
            MPLib.map[Integer, Option[cmd[A]]](highestVoted, insts)
          val newCmds: List[cmd[A]] = construct_msg[A](cmdOptions)
          val msgs: List[msg[A]] =
            MPLib.map[Integer,
                       msg[A]](((i: Integer) =>
                                 Phase2a[A](i, bal,
     list_nth[cmd[A]](newCmds, minus_int(i, startI)))),
                                insts)
          val s3: state_ext[A, Unit] =
            MPLib.fold[Integer,
                        state_ext[A, Unit]](((i: Integer) =>
      (sa: state_ext[A, Unit]) =>
      instances_update[A, Unit](((_: MPLib.finfun[Integer,
           consensus_ext[A, Unit]])
                                   =>
                                  MPLib.finfun_update[Integer,
               consensus_ext[A, Unit]](instances[A, Unit](sa), i,
val_update[A, Unit](((_: Option[cmd[A]]) =>
                      Some[cmd[A]](list_nth[cmd[A]](newCmds,
              minus_int(i, startI)))),
                     status_update[A, Unit](((_: Integer) => 1),
     accepts_update[A, Unit](((_: List[Integer]) => List(a)),
                              view_update[A,
   Unit](((_: Integer) => bal),
          ins_update[A, Unit](((_: Integer) => i),
                               MPLib.finfun_apply[Integer,
           consensus_ext[A, Unit]](instances[A, Unit](sa), i)))))))),
                                 sa)),
     insts, s2)
          val pckts: List[List[packet[A]]] =
            MPLib.map[msg[A],
                       List[packet[A]]](((m: msg[A]) =>
  send_all[A, A, Unit](a, m, s3)),
 msgs);
          (s3, MPLib.fold[List[packet[A]],
                           List[packet[A]]](((aa: List[packet[A]]) =>
      (b: List[packet[A]]) => aa ++ b),
     pckts, Nil))
        }
        else (s1, Nil))
    }
    else (s, Nil))

def def_receive2a[A : MPLib.equal](i: Integer, b: Integer, v: cmd[A],
                                    l: Integer, s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  (if (less_eq_int(ballot[A, Unit](s), b))
    {
      val a: Integer = id[A, Unit](s);
      (def_receive2[A](i, b, v, l, s),
        send_all[A, A, Unit](a, Phase2b[A](i, b, v), s))
    }
    else (s, Nil))

def def_receive2b[A : MPLib.equal](i: Integer, b: Integer, a2: Integer,
                                    v: cmd[A], s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  (def_receive2[A](i, b, v, a2, s), Nil)

def def_setStatus[A, B](s: Integer, cs: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  status_update[A, B](((_: Integer) => s), cs)

def def_getAccepts[A, B](cs: consensus_ext[A, B]): List[Integer] =
  accepts[A, B](cs)

def def_receiveFwd[A : MPLib.equal](v: A, s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  {
    val a: Integer = id[A, Unit](s);
    (if (equal_int(def_leaderOfBal(ballot[A, Unit](s),
   def_replicaCount[A, Unit](s)),
                           a) &&
           leader[A, Unit](s))
      def_send2a[A, Unit](next_inst[A, Unit](s), Comd[A](v), s) else (s, Nil))
  }

def def_setAccepts[A, B](as: List[Integer], cs: consensus_ext[A, B]):
      consensus_ext[A, B]
  =
  accepts_update[A, B](((_: List[Integer]) => as), cs)

def emptyInstances[A]: MPLib.finfun[Integer, consensus_ext[A, Unit]] =
  MPLib.finfun_const[consensus_ext[A, Unit],
                      Integer](consensus_exta[A,
         Unit](0, 0, Nil, 0, None, ()))

def def_getInstances[A, B](s: state_ext[A, B]):
      MPLib.finfun[Integer, consensus_ext[A, Unit]]
  =
  instances[A, B](s)

def def_getNextInstance[A, B](s: state_ext[A, B]): Integer =
  next_inst[A, B](s)

def processExternalEvent[A : MPLib.equal](sender: Integer, msg: msg[A],
   s: state_ext[A, Unit]):
      (state_ext[A, Unit], List[packet[A]])
  =
  (msg match {
     case Phase1a(b, i) => def_receive1a[A](sender, b, i, s)
     case Phase1b(last_vote, b) => def_receive1b[A](last_vote, b, sender, s)
     case Phase2a(i, b, cm) => def_receive2a[A](i, b, cm, sender, s)
     case Phase2b(i, b, cm) => def_receive2b[A](i, b, sender, cm, s)
     case Fwd(v) => def_receiveFwd[A](v, s)
   })

def def_getFirstUncommitted[A, B](s: state_ext[A, B]): Integer =
  firstUncommitted[A, B](s)

} /* object MultiPaxos5 */
