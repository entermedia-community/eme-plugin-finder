@import java.util.List
@import java.util.Map

!{
    var tips = java.util.List.of(
        new String[]{"Run Smart Organizer",      "magic",         "Use rules to automatically file new content into folders and collections."},
        new String[]{"Complete Your Profile",    "person-circle", "Update your account details and preferred language/theme settings."},
        new String[]{"Create Starter Collections","collection",   "Make a few top-level collections so your media is easy to browse."},
        new String[]{"Upload Your First Files",  "cloud-upload",  "Add a few sample files to verify your workflow and metadata setup."}
    );
}

@for(var tip : tips)
  <div class="col-md-6 col-lg-3 mb-4">
    <div class="card h-100 border-0 shadow-sm">
      <div class="card-body text-center">
        <i class="mb-2 text-primary bi bi-${tip[1]}" style="font-size: 32px;"></i>
        <h5 class="card-title mb-4">${tip[0]}</h5>
        <p class="card-text text-muted">${tip[2]}</p>
      </div>
    </div>
  </div>
@endfor