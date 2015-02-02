from django.conf.urls import patterns, include, url

from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'reflecoDjangoServer.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^admin/', include(admin.site.urls)),
    url(r'^$', 'reflecoSearch.views.landingPage', name='home'),
    url(r'^search/', 'reflecoSearch.views.search', name='search'),
    url(r'^results/(?P<query>[\w|\W]*)$', 'reflecoSearch.views.results', name='results'),
    url(r'^signup/', 'reflecoSearch.views.signup', name='signup'),
)
