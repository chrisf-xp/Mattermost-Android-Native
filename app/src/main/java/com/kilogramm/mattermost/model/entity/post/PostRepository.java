package com.kilogramm.mattermost.model.entity.post;

import com.kilogramm.mattermost.model.RealmSpecification;
import com.kilogramm.mattermost.model.Repository;
import com.kilogramm.mattermost.model.Specification;

import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Evgeny on 19.09.2016.
 */
public class PostRepository implements Repository<Post> {


    @Override
    public void add(Post item) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(item));
        realm.close();
    }

    @Override
    public void add(Collection<Post> items) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(items));
        realm.close();
    }

    @Override
    public void update(Post item) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> realm.insertOrUpdate(item));
        realm.close();
    }

    @Override
    public void remove(Post item) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            final Post post = realm.where(Post.class).equalTo("id",item.getId()).findFirst();
            post.deleteFromRealm();
        });
        realm.close();
    }

    @Override
    public void remove(Specification specification) {
        final RealmSpecification realmSpecification = (RealmSpecification) specification;
        final  Realm realm = Realm.getDefaultInstance();
        final  RealmResults<Post> realmResults = realmSpecification.toRealmResults(realm);

        realm.executeTransaction(realm1 -> realmResults.deleteAllFromRealm());


        realm.close();
    }

    @Override
    public RealmResults<Post> query(Specification specification) {
        final RealmSpecification realmSpecification = (RealmSpecification) specification;
        final Realm realm = Realm.getDefaultInstance();
        final RealmResults<Post> realmResults = realmSpecification.toRealmResults(realm);

        realm.close();

        return realmResults;
    }

    public void removeTempPost(String sendedPostId){
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            final Post post = realm.where(Post.class).equalTo("pendingPostId",sendedPostId).findFirst();
            post.deleteFromRealm();
        });
        realm.close();
    }
}
