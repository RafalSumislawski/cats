package cats
package free

import cats.arrow.FunctionK
import cats.data.AndThen

/**
 * The dual view of the Yoneda lemma. The free functor on `F`.
 * This is isomorphic to `F` as long as `F` itself is a functor.
 * The function from `F[A]` to `Coyoneda[F,A]` exists even when
 * `F` is not a functor.
 * Implemented using a List of functions for stack-safety.
 */
sealed abstract class Coyoneda[F[_], A] extends Serializable { self =>

  /**
   * The pivot between `fi` and `k`, usually existential.
   */
  type Pivot

  /**
   * The underlying value.
   */
  val fi: F[Pivot]

  /**
   * The list of transformer functions composed into a single function, to be lifted into `F` by `run`.
   */
  def k: Pivot => A

  import Coyoneda.Aux

  /**
   * Converts to `F[A]` given that `F` is a functor
   */
  final def run(implicit F: Functor[F]): F[A] = F.map(fi)(k)

  /**
   * Converts to `G[A]` given that `G` is a functor
   */
  final def foldMap[G[_]](trans: F ~> G)(implicit G: Functor[G]): G[A] =
    G.map(trans(fi))(k)

  /**
   * Converts to `Yoneda[F,A]` given that `F` is a functor
   */
  final def toYoneda(implicit F: Functor[F]): Yoneda[F, A] =
    new Yoneda[F, A] {
      def apply[B](f: A => B): F[B] = F.map(fi)(k.andThen(f))
    }

  /**
   * Simple function composition. Allows map fusion without touching
   * the underlying `F`.
   */
  final def map[B](f: A => B): Aux[F, B, Pivot] =
    Coyoneda(fi)(AndThen(k).andThen(f))

  /**
   * Modify the context `F` using transformation `f`.
   */
  final def mapK[G[_]](f: F ~> G): Aux[G, A, Pivot] =
    Coyoneda(f(fi))(k)

  @deprecated("Use mapK", "1.0.0-RC2")
  final private[free] def transform[G[_]](f: FunctionK[F, G]): Aux[G, A, Pivot] =
    mapK(f)

}

object Coyoneda {

  /**
   * Lift the `Pivot` type member to a parameter. It is usually more
   * convenient to use `Aux` than a structural type.
   */
  type Aux[F[_], A, B] = Coyoneda[F, A] { type Pivot = B }

  /**
   * `F[A]` converts to `Coyoneda[F,A]` for any `F`
   */
  def lift[F[_], A](fa: F[A]): Aux[F, A, A] = apply(fa)(identity[A])

  /**
   * Like `lift(fa).map(k0)`.
   */
  def apply[F[_], A, B](fa: F[A])(k0: A => B): Aux[F, B, A] =
    new Coyoneda[F, B] {
      type Pivot = A
      val k = k0
      val fi = fa
    }

  /**
   * As the free functor, `Coyoneda[F, *]` provides a functor for any `F`.
   */
  implicit def catsFreeFunctorForCoyoneda[F[_]]: Functor[Coyoneda[F, *]] =
    new Functor[Coyoneda[F, *]] {
      def map[A, B](cfa: Coyoneda[F, A])(f: A => B): Coyoneda[F, B] = cfa.map(f)
    }

}
