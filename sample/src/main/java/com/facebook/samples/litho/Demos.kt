/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho

import android.content.Context
import android.content.Intent
import com.facebook.litho.Component
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentCreator
import com.facebook.samples.litho.java.animations.animatedbadge.AnimatedBadge
import com.facebook.samples.litho.java.animations.animationcallbacks.AnimationCallbacksActivity
import com.facebook.samples.litho.java.animations.animationcomposition.ComposedAnimationsComponent
import com.facebook.samples.litho.java.animations.animationcookbook.AnimationCookBookActivity
import com.facebook.samples.litho.java.animations.bounds.BoundsAnimationComponent as JavaBoundsAnimationComponent
import com.facebook.samples.litho.java.animations.commondynamicprops.CommonDynamicPropsAnimationActivity
import com.facebook.samples.litho.java.animations.docs.AlphaTransitionComponent
import com.facebook.samples.litho.java.animations.docs.AppearTransitionComponent
import com.facebook.samples.litho.java.animations.docs.ParallelTransitionWithAnimatorsComponent
import com.facebook.samples.litho.java.animations.docs.SequenceTransitionLoopComponent
import com.facebook.samples.litho.java.animations.docs.SimpleAllLayoutTransitionComponent
import com.facebook.samples.litho.java.animations.docs.StaggerTransitionComponent
import com.facebook.samples.litho.java.animations.docs.StaggerTransitionSameComponent
import com.facebook.samples.litho.java.animations.docs.StaggerTransitionWithDelayComponent
import com.facebook.samples.litho.java.animations.docs.keyscope.GlobalKeyParentComponent
import com.facebook.samples.litho.java.animations.expandableelement.ExpandableElementActivity
import com.facebook.samples.litho.java.animations.pageindicators.PageIndicatorsRootComponent
import com.facebook.samples.litho.java.animations.sharedelements.SharedElementsComponent
import com.facebook.samples.litho.java.animations.sharedelements.SharedElementsFragmentActivity
import com.facebook.samples.litho.java.animations.transitions.TransitionsActivity
import com.facebook.samples.litho.java.bordereffects.BorderEffectsComponent
import com.facebook.samples.litho.java.changesetdebug.ItemsRerenderingActivity
import com.facebook.samples.litho.java.changesetdebug.PropUpdatingActivity
import com.facebook.samples.litho.java.changesetdebug.ScrollingToBottomActivity
import com.facebook.samples.litho.java.changesetdebug.StateResettingActivity
import com.facebook.samples.litho.java.communicating.CommunicatingBetweenSiblings
import com.facebook.samples.litho.java.communicating.CommunicatingFromChildToParent
import com.facebook.samples.litho.java.communicating.CommunicatingFromParentToChild
import com.facebook.samples.litho.java.duplicatestate.DuplicateState
import com.facebook.samples.litho.java.dynamicprops.DynamicPropsActivity
import com.facebook.samples.litho.java.editor.SimpleEditorExampleActivity
import com.facebook.samples.litho.java.errors.ErrorHandlingActivity
import com.facebook.samples.litho.java.fastscroll.FastScrollHandleComponent
import com.facebook.samples.litho.java.horizontalscroll.HorizontalScrollRootComponent
import com.facebook.samples.litho.java.hscroll.HorizontalScrollWithDynamicItemHeight
import com.facebook.samples.litho.java.hscroll.HorizontalScrollWithSnapActivity
import com.facebook.samples.litho.java.identity.ComponentIdentityActivity
import com.facebook.samples.litho.java.incrementalmount.IncrementalMountWithCustomViewContainerActivity
import com.facebook.samples.litho.java.lifecycle.LifecycleDelegateActivity
import com.facebook.samples.litho.java.lifecycle.LifecycleFragmentActivity
import com.facebook.samples.litho.java.lifecycle.ViewPagerLifecycleActivity
import com.facebook.samples.litho.java.lithography.LithographyActivity
import com.facebook.samples.litho.java.onboarding.IntroducingLayoutComponent
import com.facebook.samples.litho.java.onboarding.LayoutWithImageComponent
import com.facebook.samples.litho.java.playground.PlaygroundComponent
import com.facebook.samples.litho.java.stateupdates.SectionStateUpdateFromComponentSection
import com.facebook.samples.litho.java.stateupdates.StateUpdateFromOutsideTreeActivity
import com.facebook.samples.litho.java.stats.Stats
import com.facebook.samples.litho.java.textinput.TextInputRequestAndClearFocus
import com.facebook.samples.litho.java.textinput.TextInputWithKeyboardAndFocusDemo
import com.facebook.samples.litho.java.triggers.ClearTextTriggerExampleComponent
import com.facebook.samples.litho.java.triggers.CustomEventTriggerExampleComponent
import com.facebook.samples.litho.java.triggers.TooltipTriggerExampleActivity
import com.facebook.samples.litho.java.viewpager.ViewPagerDemoComponent
import com.facebook.samples.litho.kotlin.animations.animatedapi.AnimatedComponent
import com.facebook.samples.litho.kotlin.animations.animatedbadge.AnimatedBadgeKotlin
import com.facebook.samples.litho.kotlin.animations.animatedcounter.AnimatingCounterRootComponent
import com.facebook.samples.litho.kotlin.animations.animationcomposition.ComposedAnimationsComponentKotlin
import com.facebook.samples.litho.kotlin.animations.bounds.BoundsAnimationComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.AllCommonDynamicPropsKComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.AnimateDynamicPropsComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.AnimateDynamicPropsKComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.CommonDynamicPropsComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.CommonDynamicPropsKComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.CustomDynamicPropsComponent
import com.facebook.samples.litho.kotlin.animations.dynamicprops.CustomDynamicPropsKComponent
import com.facebook.samples.litho.kotlin.animations.expandableelement.ExpandableElementRootKotlinKComponent
import com.facebook.samples.litho.kotlin.animations.messages.Message
import com.facebook.samples.litho.kotlin.animations.transitions.TransitionsComponent
import com.facebook.samples.litho.kotlin.bordereffects.BorderEffectsComponentKotlin
import com.facebook.samples.litho.kotlin.canvas.DrawAnimatedSquareCanvasKComponent
import com.facebook.samples.litho.kotlin.canvas.DrawPathCanvasKComponent
import com.facebook.samples.litho.kotlin.canvas.DrawRotatedGroupCanvasKComponent
import com.facebook.samples.litho.kotlin.canvas.DrawShapeCanvasKComponent
import com.facebook.samples.litho.kotlin.canvas.DrawStrokedShapeCanvasKComponent
import com.facebook.samples.litho.kotlin.canvas.DrawTransparentHoleCanvasKComponent
import com.facebook.samples.litho.kotlin.canvas.SimpleImageViewExamplePrimitiveComponent
import com.facebook.samples.litho.kotlin.collection.ChangeableItemsCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.CollectionKComponent
import com.facebook.samples.litho.kotlin.collection.DepsCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.FriendsCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.HorizontalScrollKComponent
import com.facebook.samples.litho.kotlin.collection.ModularCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.MultiListCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.PaginationCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.PullToRefreshCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.ScrollToCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.SelectionCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.SpanCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.StaggeredGridCollectionExample
import com.facebook.samples.litho.kotlin.collection.StickyHeaderCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.TabsCollectionKComponent
import com.facebook.samples.litho.kotlin.errors.ErrorHandlingKotlinActivity
import com.facebook.samples.litho.kotlin.gettingstarted.BasicList
import com.facebook.samples.litho.kotlin.gettingstarted.ClickableText
import com.facebook.samples.litho.kotlin.gettingstartedsolution.VerticalSpeller
import com.facebook.samples.litho.kotlin.lithography.LithographyKotlinActivity
import com.facebook.samples.litho.kotlin.logging.LoggingActivity
import com.facebook.samples.litho.kotlin.observability.UseFlowComponent
import com.facebook.samples.litho.kotlin.observability.UseLiveDataComponent
import com.facebook.samples.litho.kotlin.playground.PlaygroundKComponent
import com.facebook.samples.litho.kotlin.primitives.SimpleCanvasExampleComponent
import com.facebook.samples.litho.kotlin.primitives.SimpleImageViewWithAccessibilityExamplePrimitiveComponent
import com.facebook.samples.litho.kotlin.primitives.bindto.PrimitiveBindToExampleComponent
import com.facebook.samples.litho.kotlin.primitives.controllers.ControllersExamplePrimitiveComponent
import com.facebook.samples.litho.kotlin.primitives.widgets.PrimitiveWidgetsExampleComponent
import com.facebook.samples.litho.kotlin.sizeconstraintsawarecomponent.SizeConstraintsAwareComponentKComponent
import com.facebook.samples.litho.kotlin.state.IdentityRootComponent
import com.facebook.samples.litho.kotlin.state.StateParentChildComponent
import com.facebook.samples.litho.kotlin.treeprops.TreePropsExampleComponent
import com.facebook.samples.litho.kotlin.triggers.TooltipTriggerExampleKComponent
import com.facebook.samples.litho.onboarding.FirstComponentActivity
import com.facebook.samples.litho.onboarding.HelloWorldActivity
import com.facebook.samples.litho.onboarding.PostStyledKComponent
import com.facebook.samples.litho.onboarding.PostWithActionsKComponent
import com.facebook.samples.litho.onboarding.UserFeedKComponent
import com.facebook.samples.litho.onboarding.UserFeedWithStoriesKComponent
import com.facebook.samples.litho.onboarding.model.FEED
import com.facebook.samples.litho.onboarding.model.NEBULAS_POST
import com.facebook.samples.litho.onboarding.model.USER_STORIES
import com.facebook.samples.litho.playground.XMLPlaygroundActivity

class Demos {
  companion object {
    @kotlin.jvm.JvmField
    val DEMOS =
        listOf(
            DemoList(
                name = "Playground",
                listOf(
                    DemoGrouping(
                        name = "Playground",
                        listOf(
                            SingleDemo(name = "Java API Playground") { context ->
                              PlaygroundComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Kotlin API Playground", component = PlaygroundKComponent()),
                            SingleDemo(
                                name = "XML Playground",
                                klass = XMLPlaygroundActivity::class.java))))),
            // Please keep this alphabetical with consistent naming and casing!
            DemoList(
                name = "Kotlin API Demos",
                listOf(
                    DemoGrouping(
                        name = "Animations",
                        listOf(
                            SingleDemo(name = "Animated API Demo", component = AnimatedComponent()),
                            SingleDemo(name = "Animated Badge", component = AnimatedBadgeKotlin()),
                            SingleDemo(
                                name = "Bounds Animation", component = BoundsAnimationComponent()),
                            SingleDemo(
                                name = "Animated Counter",
                                component = AnimatingCounterRootComponent()),
                            SingleDemo(
                                name = "Animations Composition",
                                component = ComposedAnimationsComponentKotlin()),
                            SingleDemo(
                                name = "Expandable Element",
                                component =
                                    ExpandableElementRootKotlinKComponent(
                                        initialMessages = Message.MESSAGES)),
                            SingleDemo(name = "Transitions", component = TransitionsComponent()),
                            SingleDemo(name = "[Spec] Common Dynamic Props") { context ->
                              CommonDynamicPropsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "[KComponent] Common Dynamic Props",
                                component = CommonDynamicPropsKComponent()),
                            SingleDemo(
                                name = "All Common Dynamic Props",
                                component = AllCommonDynamicPropsKComponent()),
                            SingleDemo(name = "[Spec] Custom Dynamic Props") { context ->
                              CustomDynamicPropsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "[KComponent] Custom Dynamic Props",
                                component = CustomDynamicPropsKComponent()),
                            SingleDemo(name = "[Spec] Animated Dynamic Props") { context ->
                              AnimateDynamicPropsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "[KComponent] Animated Dynamic Props",
                                component = AnimateDynamicPropsKComponent()),
                            SingleDemo(
                                name = "[KComponent] SizeConstraintsAwareComponent",
                                component = SizeConstraintsAwareComponentKComponent()),
                        )),
                    DemoGrouping(
                        name = "Getting started",
                        listOf(
                            SingleDemo(name = "Clickable Text", component = ClickableText("Litho")),
                            SingleDemo(name = "Basic List", component = BasicList()),
                            SingleDemo(
                                name = "Vertical Speller (Solution)",
                                component = VerticalSpeller("I❤️Litho")))),
                    DemoGrouping(
                        name = "Collections",
                        listOf(
                            SingleDemo(name = "Fixed Items", component = CollectionKComponent()),
                            SingleDemo(
                                name = "Changeable items",
                                component = ChangeableItemsCollectionKComponent()),
                            SingleDemo(
                                name = "Scroll to items",
                                component = ScrollToCollectionKComponent()),
                            SingleDemo(
                                name = "Pull to refresh",
                                component = PullToRefreshCollectionKComponent()),
                            SingleDemo(
                                name = "Sticky Header",
                                component = StickyHeaderCollectionKComponent()),
                            SingleDemo(name = "Span", component = SpanCollectionKComponent()),
                            SingleDemo(
                                name = "MultiList", component = MultiListCollectionKComponent()),
                            SingleDemo(
                                name = "Pagination", component = PaginationCollectionKComponent()),
                            SingleDemo(name = "Deps", component = DepsCollectionKComponent()),
                            SingleDemo(
                                name = "Selection", component = SelectionCollectionKComponent()),
                            SingleDemo(
                                name = "Modular Collection",
                                component = ModularCollectionKComponent()),
                            SingleDemo(name = "Friends", component = FriendsCollectionKComponent()),
                            SingleDemo(
                                name = "Horizontal Scroll",
                                component = HorizontalScrollKComponent()),
                            SingleDemo(name = "Tabs", component = TabsCollectionKComponent()),
                            SingleDemo(
                                name = "Staggered Grid",
                                component = StaggeredGridCollectionExample()),
                            SingleDemo(
                                name = "Sections Demo: Lithography",
                                klass = LithographyKotlinActivity::class.java))),
                    DemoGrouping(
                        name = "Primitives",
                        listOf(
                            SingleDemo(
                                name = "Simple ImageView Primitive Component Example",
                                component = SimpleImageViewExamplePrimitiveComponent()),
                            SingleDemo(
                                name = "Simple ImageView Primitive Component With A11Y Example",
                                component =
                                    SimpleImageViewWithAccessibilityExamplePrimitiveComponent()),
                            SingleDemo(
                                name = "BindTo - Primitive Dynamic Values API",
                                component = PrimitiveBindToExampleComponent()),
                            SingleDemo(
                                name = "Sample Primitive Components Example",
                                component = PrimitiveWidgetsExampleComponent()),
                            SingleDemo(
                                name = "Controllers",
                                component = ControllersExamplePrimitiveComponent()),
                        )),
                    DemoGrouping(
                        name = "Canvas",
                        listOf(
                            SingleDemo(
                                name = "Canvas Component Example",
                                component = SimpleCanvasExampleComponent()),
                            SingleDemo(
                                name = "Draw Shape", component = DrawShapeCanvasKComponent()),
                            SingleDemo(name = "Draw Path", component = DrawPathCanvasKComponent()),
                            SingleDemo(
                                name = "Draw Stroked Shape",
                                component = DrawStrokedShapeCanvasKComponent()),
                            SingleDemo(
                                name = "Draw Rotated Group",
                                component = DrawRotatedGroupCanvasKComponent()),
                            SingleDemo(
                                name = "Draw Transparent Hole",
                                component = DrawTransparentHoleCanvasKComponent()),
                            SingleDemo(
                                name = "Draw Animated Square",
                                component = DrawAnimatedSquareCanvasKComponent()),
                        )),
                    DemoGrouping(
                        name = "Errors",
                        listOf(
                            SingleDemo(
                                name = "[KComponent] Error Boundaries",
                                klass = ErrorHandlingKotlinActivity::class.java))),
                    DemoGrouping(
                        name = "State",
                        listOf(
                            SingleDemo(
                                name = "Updating State<T> from child component",
                                component = StateParentChildComponent()),
                            SingleDemo(
                                name = "Component Identity", component = IdentityRootComponent()))),
                    DemoGrouping(
                        name = "Common Props",
                        listOf(
                            SingleDemo(
                                name = "Border Effects",
                                component = BorderEffectsComponentKotlin()),
                            SingleDemo(
                                name = "TreeProps", component = TreePropsExampleComponent()))),
                    DemoGrouping(
                        name = "Triggers",
                        listOf(
                            SingleDemo(
                                name = "Tooltip Trigger",
                                component = TooltipTriggerExampleKComponent()))),
                    DemoGrouping(
                        name = "Logging",
                        listOf(SingleDemo(name = " Logging", klass = LoggingActivity::class.java))),
                    DemoGrouping(
                        name = "Observability",
                        listOf(
                            SingleDemo(name = "useLiveData", component = UseLiveDataComponent()),
                            SingleDemo(name = "useFlow", component = UseFlowComponent()))))),
            DemoList(
                name = "Java API Demos",
                listOf(
                    DemoGrouping(
                        name = "Animations",
                        listOf(
                            SingleDemo(
                                name = "Animations Composition",
                            ) { context ->
                              ComposedAnimationsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Expandable Element", ExpandableElementActivity::class.java),
                            SingleDemo(name = "Animated Badge") { context ->
                              AnimatedBadge.create(context).build()
                            },
                            SingleDemo(name = "Bounds Animation") { context ->
                              JavaBoundsAnimationComponent.create(context).build()
                            },
                            SingleDemo(name = "Page Indicators") { context ->
                              PageIndicatorsRootComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Common Dynamic Props Animations",
                                klass = CommonDynamicPropsAnimationActivity::class.java),
                            SingleDemo(
                                name = "Animation Cookbook",
                                klass = AnimationCookBookActivity::class.java),
                            SingleDemo(
                                name = "Animation Callbacks",
                                klass = AnimationCallbacksActivity::class.java),
                            SingleDemo(name = "Activity Transition with Shared elements") { context
                              ->
                              SharedElementsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Fragments Transition with Shared elements",
                                klass = SharedElementsFragmentActivity::class.java),
                            SingleDemo(
                                name = "Transitions", klass = TransitionsActivity::class.java),
                            SingleDemo(name = "All Layout Transition") { context ->
                              SimpleAllLayoutTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Alpha Transition") { context ->
                              AlphaTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Appear Transition") { context ->
                              AppearTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Stagger Transition") { context ->
                              StaggerTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Stagger Transition on same Component") { context ->
                              StaggerTransitionSameComponent.create(context).build()
                            },
                            SingleDemo(name = "Stagger Transition with Delay") { context ->
                              StaggerTransitionWithDelayComponent.create(context).build()
                            },
                            SingleDemo(name = "Parallel Transition with Animators") { context ->
                              ParallelTransitionWithAnimatorsComponent.create(context).build()
                            },
                            SingleDemo(name = "Sequence Transition loop") { context ->
                              SequenceTransitionLoopComponent.create(context).build()
                            },
                            SingleDemo(name = "Global key Transition") { context ->
                              GlobalKeyParentComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Collections",
                        listOf(
                            SingleDemo(name = "HorizontalScroll (non-recycling)") { context ->
                              HorizontalScrollRootComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Sections Demo: Lithography",
                                klass = LithographyActivity::class.java),
                            SingleDemo(
                                name = "Snapping",
                                klass = HorizontalScrollWithSnapActivity::class.java),
                            SingleDemo(name = "Dynamic Item Height") { context ->
                              HorizontalScrollWithDynamicItemHeight.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Common Props",
                        listOf(
                            SingleDemo(name = "Border Effects") { context ->
                              BorderEffectsComponent.create(context).build()
                            },
                            SingleDemo(name = "Duplicate Parent/Child State") { context ->
                              DuplicateState.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Dynamic Props",
                        listOf(
                            SingleDemo(
                                name = "Dynamic Props Demo",
                                klass = DynamicPropsActivity::class.java),
                            SingleDemo(name = "Fast Scroll Handle") { context ->
                              FastScrollHandleComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Incremental Mount",
                        listOf(
                            SingleDemo(
                                name = "With Custom Animating Container",
                                klass =
                                    IncrementalMountWithCustomViewContainerActivity::class.java))),
                    DemoGrouping(
                        name = "Lifecycle",
                        listOf(
                            SingleDemo(
                                name = "Error Boundaries",
                                klass = ErrorHandlingActivity::class.java),
                            SingleDemo(
                                name = "Lifecycle Callbacks",
                                klass = LifecycleDelegateActivity::class.java),
                            SingleDemo(
                                name = "ViewPager Lifecycle Callbacks",
                                klass = ViewPagerLifecycleActivity::class.java),
                            SingleDemo(
                                name = "Fragment transactions lifecycle",
                                klass = LifecycleFragmentActivity::class.java))),
                    DemoGrouping(
                        name = "Other Widgets",
                        listOf(
                            SingleDemo(name = "ViewPager") { context ->
                              ViewPagerDemoComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "State Updates",
                        listOf(
                            SingleDemo(
                                name = "State Update from Outside Litho",
                                klass = StateUpdateFromOutsideTreeActivity::class.java),
                            SingleDemo(name = "State Update in Section from Child Component") {
                                context ->
                              RecyclerCollectionComponent.create(context)
                                  .disablePTR(true)
                                  .section(
                                      SectionStateUpdateFromComponentSection.create(
                                              SectionContext(context))
                                          .build())
                                  .build()
                            },
                            SingleDemo(
                                name = "State and identity",
                                klass = ComponentIdentityActivity::class.java),
                        )),
                    DemoGrouping(
                        name = "Communicating between Components",
                        listOf(
                            SingleDemo(
                                name = "From child to parent",
                                klass = CommunicatingFromChildToParent::class.java),
                            SingleDemo(
                                name = "From parent to child",
                                klass = CommunicatingFromParentToChild::class.java),
                            SingleDemo(
                                name = "Between sibling components",
                                klass = CommunicatingBetweenSiblings::class.java),
                        )),
                    DemoGrouping(
                        name = "TextInput",
                        listOf(
                            SingleDemo(name = "Focus and Show Soft Keyboard on Appear") { context ->
                              TextInputWithKeyboardAndFocusDemo.create(context).build()
                            },
                            SingleDemo(name = "Request and Clear Focus with Keyboard") { context ->
                              TextInputRequestAndClearFocus.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Triggers",
                        listOf(
                            SingleDemo(name = "Clear Text Trigger") { context ->
                              ClearTextTriggerExampleComponent.create(context).build()
                            },
                            SingleDemo(name = "Custom Event Trigger") { context ->
                              CustomEventTriggerExampleComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Tooltip Trigger",
                                klass = TooltipTriggerExampleActivity::class.java))),
                    DemoGrouping(
                        name = "Editor",
                        listOf(
                            SingleDemo(
                                name = "SimpleEditor for Props and State",
                                klass = SimpleEditorExampleActivity::class.java))))),
            DemoList(
                name = "Internal Debugging Samples",
                listOf(
                    DemoGrouping(
                        name = "Litho Stats",
                        listOf(
                            SingleDemo(name = "LithoStats") { context ->
                              Stats.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Sections Changesets",
                        listOf(
                            SingleDemo(
                                name = "Resetting state",
                                klass = StateResettingActivity::class.java),
                            SingleDemo(
                                name = "Items re-rendering",
                                klass = ItemsRerenderingActivity::class.java),
                            SingleDemo(
                                name = "Not updating with new props",
                                klass = PropUpdatingActivity::class.java),
                            SingleDemo(
                                name = "List scrolls to bottom",
                                klass = ScrollingToBottomActivity::class.java))))),
            DemoList(
                name = "Tutorial",
                listOf(
                    DemoGrouping(
                        name = "Onboarding",
                        listOf(
                            SingleDemo(
                                name = "1. Hello World", klass = HelloWorldActivity::class.java),
                            SingleDemo(
                                name = "2. First Litho Component",
                                klass = FirstComponentActivity::class.java),
                            SingleDemo(name = "3. Introducing Layout") { context ->
                              IntroducingLayoutComponent.create(context).name("Linda").build()
                            },
                            SingleDemo(name = "3.1. More with Layout") { context ->
                              LayoutWithImageComponent.create(context).name("Linda").build()
                            },
                            SingleDemo(name = "3.2. Flexbox Styling") {
                              PostStyledKComponent(post = NEBULAS_POST)
                            },
                            SingleDemo(name = "4. Add State") {
                              PostWithActionsKComponent(post = NEBULAS_POST)
                            },
                            SingleDemo(name = "5. List") { UserFeedKComponent(posts = FEED) },
                            SingleDemo(name = "5.1. List within Lists") {
                              UserFeedWithStoriesKComponent(
                                  posts = FEED, usersWithStories = USER_STORIES)
                            },
                        )))))
  }

  interface DemoItem {
    val name: String
  }

  /**
   * The reasons indices are used is so we have something parcelable to pass to the Activity (a
   * ComponentCreator is not parcelable).
   */
  interface NavigableDemoItem : DemoItem {
    fun getIntent(context: Context?, currentIndices: IntArray?): Intent
  }

  interface HasChildrenDemos {
    val demos: List<DemoItem>?
  }

  /** A DemoList has groupings of SingleDemos or DemoLists to navigate to. */
  class DemoList(override val name: String, val datamodels: List<DemoGrouping>) :
      NavigableDemoItem, HasChildrenDemos {
    override fun getIntent(context: Context?, currentIndices: IntArray?): Intent {
      val intent = Intent(context, DemoListActivity::class.java)
      intent.putExtra(DemoListActivity.INDICES, currentIndices)
      return intent
    }

    override val demos: List<DemoGrouping>?
      get() = datamodels
  }

  /** A DemoGrouping is a list of demo items that show under a single heading. */
  class DemoGrouping
  internal constructor(override val name: String, val datamodels: List<NavigableDemoItem>?) :
      DemoItem, HasChildrenDemos {
    override val demos: List<DemoItem>?
      get() = datamodels
  }

  class SingleDemo : NavigableDemoItem {
    override val name: String
    val klass: Class<*>?
    val componentCreator: ComponentCreator?
    val component: Component?

    internal constructor(
        name: String,
        klass: Class<*>? = null,
        component: Component? = null,
        componentCreator: ComponentCreator? = null
    ) {
      this.name = name
      this.klass = klass
      this.componentCreator = componentCreator
      this.component = component
    }

    private val activityClass: Class<*>
      private get() = klass ?: ComponentDemoActivity::class.java

    override fun getIntent(context: Context?, currentIndices: IntArray?): Intent {
      val intent = Intent(context, activityClass)
      intent.putExtra(DemoListActivity.INDICES, currentIndices)
      return intent
    }
  }
}
