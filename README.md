# BaseAdapterWithCaching
Caching specific view types in recycler view


https://medium.com/@sumeetmay11/recycler-view-and-view-level-caching-f06e3fd6391b

The Scenario
So, recently i was going through my app , which contains a vertical list of items and if you click on certain item , then we need to show suggestions based on this item . These suggestions we are showing in a horizontal carousel and we are fetching them from server . We as usual using recycler view for showing all this . Everything all good .

Technical picture
Now ,let’s go into technicalities , So the logic for fetching the suggestions and showing in a carousel , is handled by a separate view holder (let’s call it suggestions view holder for this article), which internally contains its own recycler view for showing items in horizontal list . And, in on bind view , this view holder is doing this task for fetching and showing in recycler view . We can’t fetch the data beforehand as we don’t know which item user will click .Plus , we cant save the data in parent adapter , as for architecture stuff we want to keep this view holder logic separate.

The Problem
Each time this view holder comes in visible area as we scroll ,then on bind is called again and we had to fetch suggestions again from server and set new adapter and show in ui.

The Solution
So , if by any means we can resist multiple on-bind calls for this suggestion view holder , then we are all good ,right . But , for this to happen we need two things . First, we need to tell recycler view to always reuse same view holder once bind ,for the same position without binding again . Second , not to use this view holder for some other position having same type. (i.e not to move it to recycler view pool).


**In simple terms we need a solution by which we can cache a specific view types .
